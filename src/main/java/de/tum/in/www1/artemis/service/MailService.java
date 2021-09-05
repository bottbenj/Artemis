package de.tum.in.www1.artemis.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.notification.GroupNotification;
import de.tum.in.www1.artemis.domain.notification.NotificationTarget;
import de.tum.in.www1.artemis.domain.notification.SingleUserNotification;
import de.tum.in.www1.artemis.repository.UserRepository;
import io.github.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails.
 * <p>
 * We use the @Async annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String GROUP = "group";

    private static final String BASE_URL = "baseUrl";

    private static final String NOTIFICATION_URL = "notificationUrl";

    private static final String GROUP_NOTIFICATION = "groupNotification";

    private final JHipsterProperties jHipsterProperties;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    private final UserRepository userRepository;

    public MailService(JHipsterProperties jHipsterProperties, JavaMailSender javaMailSender, MessageSource messageSource, SpringTemplateEngine templateEngine,
            UserRepository userRepository) {
        this.jHipsterProperties = jHipsterProperties;
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;

        this.userRepository = userRepository;
    }

    /**
     * Sends an e-mail to the specified sender
     *
     * @param isGroupEmail indicates if the email will be send to an individual or a group
     * @param users who should be contacted.
     * @param subject The mail subject
     * @param content The content of the mail. Can be enriched with HTML tags
     * @param isMultipart Whether to create a multipart that supports alternative texts, inline elements
     * @param isHtml Whether the mail should support HTML tags
     */
    @Async
    public void sendEmail(boolean isGroupEmail, List<User> users, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug("Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}", isMultipart, isHtml, users, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            if (!isGroupEmail) {
                message.setTo(users.get(0).getEmail());
            }
            else {
                String[] bcc = users.stream().map(User::getEmail).toArray(String[]::new);
                message.setBcc(bcc);
            }
            message.setFrom(jHipsterProperties.getMail().getFrom());
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            log.info("Sent email with subject '{}' to User '{}'", subject, users);
        }
        catch (MailException | MessagingException e) {
            log.warn("Email could not be sent to user '{}'", users, e);
        }
    }

    /**
     * Sends a predefined mail based on a template
     *
     * @param user The receiver of the mail
     * @param templateName The name of the template
     * @param titleKey The key mapping the title for the subject of the mail
     */
    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        Context context = this.prepareContext(user);
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, context.getLocale());
        sendEmail(false, Collections.singletonList(user), subject, content, false, true);
    }

    private Context prepareContext(User user) {
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        return context;
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/passwordResetEmail", "email.reset.title");
    }

    @Async
    public void sendSAML2SetPasswordMail(User user) {
        log.debug("Sending SAML2 set password email to '{}'", user.getEmail());
        sendEmailFromTemplate(user, "mail/samlSetPasswordEmail", "email.saml.title");
    }

    // notification related

    /**
     * Creates and sends an email to a single user based on a SingleUserNotification
     * @param notification which information should also be propagated via email
     */
    @Async
    public void sendSingleUserNotificationEmail(SingleUserNotification notification) {
        User user = notification.getRecipient();
        log.debug("Sending notification email to '{}'", user.getEmail());

        Context context = this.prepareContext(user);
        context.setVariable(GROUP_NOTIFICATION, notification);

        String content = templateEngine.process("mail/notifications/notificationEmailTest", context);
        String subject = notification.getTitle();

        sendEmail(false, Collections.singletonList(user), subject, content, false, true);
    }

    /**
     * Creates and sends an email to a group of users based on a GroupNotification
     * @param notification which information should also be propagated via email
     * @param userList with the users that should receive an email (via bcc) (i.e. only one email is created)
     */
    @Async
    public void sendGroupNotificationEmail(GroupNotification notification, List<User> userList) {
        log.debug("Sending group notification email");

        Locale locale = Locale.forLanguageTag("en");
        Context context = new Context(locale);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        context.setVariable(GROUP_NOTIFICATION, notification);
        context.setVariable(NOTIFICATION_URL, NotificationTarget.extractNotificationUrl(notification));

        String content = templateEngine.process("mail/notifications/groupNotificationEmailTests", context);
        String subject = notification.getTitle();

        sendEmail(true, userList, subject, content, false, true);
    }

}

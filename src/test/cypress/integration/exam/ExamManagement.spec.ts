import { POST } from '../../support/constants';
import { CypressExamBuilder } from '../../support/requests/CourseManagementRequests';
import { artemis } from '../../support/ArtemisTesting';
import { generateUUID } from '../../support/utils';

// Requests
const courseManagementRequests = artemis.requests.courseManagement;

// User management
const users = artemis.users;

// Pageobjects
const navigationBar = artemis.pageobjects.navigationBar;
const courseManagement = artemis.pageobjects.courseManagement;
const examManagement = artemis.pageobjects.examManagement;
const textCreation = artemis.pageobjects.textExercise.creation;
const exerciseGroups = artemis.pageobjects.examExerciseGroups;
const exerciseGroupCreation = artemis.pageobjects.examExerciseGroupCreation;

// Common primitives
const uid = generateUUID();
const examTitle = 'exam' + uid;

describe('Exam management', () => {
    let course: any;
    let exam: any;

    before(() => {
        cy.login(users.getAdmin());
        courseManagementRequests.createCourse().then((response) => {
            course = response.body;
            courseManagementRequests.addStudentToCourse(course.id, users.getStudentOne().username);
            const examConfig = new CypressExamBuilder(course).title(examTitle).build();
            courseManagementRequests.createExam(examConfig).then((examResponse) => {
                exam = examResponse.body;
            });
        });
    });

    beforeEach(() => {
        cy.login(users.getAdmin());
    });

    it('Adds an exercise group with a text exercise', () => {
        cy.visit('/');
        navigationBar.openCourseManagement();
        courseManagement.openExamsOfCourse(course.title, course.shortName);
        examManagement.getExamRow(examTitle).openExerciseGroups();
        exerciseGroups.shouldShowNumberOfExerciseGroups(0);
        exerciseGroups.clickAddExerciseGroup();
        const groupName = 'group 1';
        exerciseGroupCreation.typeTitle(groupName);
        exerciseGroupCreation.isMandatoryBoxShouldBeChecked();
        exerciseGroupCreation.clickSave();
        exerciseGroups.shouldShowNumberOfExerciseGroups(1);
        // Add text exercise
        exerciseGroups.clickAddTextExercise();
        const textExerciseTitle = 'text' + uid;
        textCreation.typeTitle(textExerciseTitle);
        textCreation.typeMaxPoints(10);
        textCreation.create().its('response.statusCode').should('eq', 201);
        exerciseGroups.visitPageViaUrl(course.id, exam.id);
        exerciseGroups.shouldContainExerciseWithTitle(textExerciseTitle);
    });

    it('Registers the course students for the exam', () => {
        // We already verified in the previous test that we can navigate here
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.getExamRow(examTitle).openStudentRegistration();
        cy.contains('Registered students: 0').should('be.visible');
        cy.intercept(POST, '/api/courses/*/exams/*/register-course-students').as('registerCourseStudents');
        cy.get('[jhitranslate="artemisApp.examManagement.examStudents.registerAllFromCourse"]').click();
        cy.wait('@registerCourseStudents').its('response.statusCode').should('eq', 200);
        cy.contains(users.getStudentOne().username).should('be.visible');
        cy.contains('Registered students: 1').should('be.visible');
    });

    it('Generates student exams', () => {
        cy.visit(`/course-management/${course.id}/exams`);
        examManagement.getExamRow(examTitle).openStudenExams();
        cy.contains('0 total').should('be.visible');
        cy.intercept(POST, '/api/courses/*/exams/*/generate-student-exams').as('generateStudentExams');
        cy.get('#generateStudentExamsButton').click();
        cy.wait('@generateStudentExams');
        cy.contains('1 total').should('be.visible');
        cy.get('#generateMissingStudentExamsButton').should('be.disabled');
    });

    after(() => {
        if (!!course) {
            cy.login(users.getAdmin());
            courseManagementRequests.deleteCourse(course.id);
        }
    });
});

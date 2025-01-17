import { getTestBed, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { take } from 'rxjs/operators';
import * as chai from 'chai';
import dayjs from 'dayjs';
import sinonChai from 'sinon-chai';
import { Result } from 'app/entities/result.model';
import { Feedback } from 'app/entities/feedback.model';
import { FileUploadAssessmentService } from 'app/exercises/file-upload/assess/file-upload-assessment.service';
import { ArtemisTestModule } from '../test.module';
import { MockSyncStorage } from '../helpers/mocks/service/mock-sync-storage.service';
import { MockTranslateService } from '../helpers/mocks/service/mock-translate.service';
import { ComplaintResponse } from 'app/entities/complaint-response.model';
import { HttpResponse } from '@angular/common/http';
import { SessionStorageService } from 'ngx-webstorage';
import { TranslateService } from '@ngx-translate/core';
chai.use(sinonChai);
const expect = chai.expect;

describe('Modeling Assessment Service', () => {
    let injector: TestBed;
    let httpMock: HttpTestingController;
    let service: FileUploadAssessmentService;
    let expectedResult: any;
    let httpExpectedResult: any;
    let elemDefault: Result;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, HttpClientTestingModule],
            providers: [
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: TranslateService, useClass: MockTranslateService },
            ],
        });
        injector = getTestBed();
        service = injector.get(FileUploadAssessmentService);
        httpMock = injector.get(HttpTestingController);

        expectedResult = {} as Result;
        httpExpectedResult = {} as HttpResponse<Result>;
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('Service methods', () => {
        describe('methods returning a result', () => {
            elemDefault = new Result();
            elemDefault.id = 1;
            elemDefault.resultString = '5 of 5 points';
            elemDefault.score = 5;
            elemDefault.hasComplaint = false;
            elemDefault.completionDate = dayjs();
            elemDefault.submission = { id: 187, submissionDate: dayjs() };
            elemDefault.participation = { id: 6, initializationDate: dayjs() };
            it('should save an assessment', async () => {
                const submissionId = 187;
                const feedbacks = [
                    {
                        id: 0,
                        credits: 3,
                        reference: 'reference',
                    } as Feedback,
                    {
                        id: 1,
                        credits: 1,
                    } as Feedback,
                ];
                const returnedFromService = Object.assign({}, elemDefault);
                service
                    .saveAssessment(feedbacks, submissionId, false)
                    .pipe(take(1))
                    .subscribe((resp) => (expectedResult = resp));
                const req = httpMock.expectOne({
                    url: `${SERVER_API_URL}api/file-upload-submissions/${submissionId}/feedback`,
                    method: 'PUT',
                });
                req.flush(returnedFromService);
                expect(expectedResult).to.deep.equal(elemDefault);
            });

            it('should submit an assessment', async () => {
                const submissionId = 187;
                const feedbacks = [
                    {
                        id: 0,
                        credits: 3,
                        reference: 'reference',
                    } as Feedback,
                    {
                        id: 1,
                        credits: 1,
                    } as Feedback,
                ];
                const returnedFromService = Object.assign({}, elemDefault);
                service
                    .saveAssessment(feedbacks, submissionId, true)
                    .pipe(take(1))
                    .subscribe((resp) => (expectedResult = resp));
                const req = httpMock.expectOne({
                    url: `${SERVER_API_URL}api/file-upload-submissions/${submissionId}/feedback?submit=true`,
                    method: 'PUT',
                });
                req.flush(returnedFromService);
                expect(expectedResult).to.deep.equal(elemDefault);
            });

            it('should get an assessment', async () => {
                const submissionId = 187;
                const returnedFromService = Object.assign({}, elemDefault);
                service
                    .getAssessment(submissionId)
                    .pipe(take(1))
                    .subscribe((resp) => (expectedResult = resp));
                const req = httpMock.expectOne({
                    url: `${SERVER_API_URL}api/file-upload-submissions/${submissionId}/result`,
                    method: 'GET',
                });
                req.flush(returnedFromService);
                expect(expectedResult).to.deep.equal(elemDefault);
            });

            it('should update assessment after complaint', async () => {
                const feedbacks = [
                    {
                        id: 0,
                        credits: 3,
                        reference: 'reference',
                    } as Feedback,
                    {
                        id: 1,
                        credits: 1,
                    } as Feedback,
                ];
                const complaintResponse = new ComplaintResponse();
                complaintResponse.id = 1;
                complaintResponse.responseText = 'That is true';
                const submissionId = 1;
                const returnedFromService = { ...elemDefault };
                const expected = { ...returnedFromService };
                service
                    .updateAssessmentAfterComplaint(feedbacks, complaintResponse, submissionId)
                    .pipe(take(1))
                    .subscribe((resp) => (httpExpectedResult = resp));
                const req = httpMock.expectOne({ url: `${SERVER_API_URL}api/file-upload-submissions/${submissionId}/assessment-after-complaint`, method: 'PUT' });
                req.flush(returnedFromService);
                expect(httpExpectedResult.body).to.deep.equal(expected);
            });
        });
        describe('methods not returning a result', () => {
            it('should cancel Assessment', () => {
                const submissionId = 187;
                service
                    .cancelAssessment(submissionId)
                    .pipe(take(1))
                    .subscribe((resp) => (expectedResult = resp));
                httpMock.expectOne({
                    url: `${SERVER_API_URL}api/file-upload-submissions/${submissionId}/cancel-assessment`,
                    method: 'PUT',
                });
                expect(expectedResult).to.deep.equal({});
            });
        });
    });
});

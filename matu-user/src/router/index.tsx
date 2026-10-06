import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '../layout/AppLayout/AppLayout'
import { StandaloneLayout } from '../layout/StandaloneLayout/StandaloneLayout'
import { RequireLogin } from './RequireLogin'
import { RequireCoursePublisher } from './RequireCoursePublisher'
import { HomePage } from '../pages/HomePage/page'
import { CheckInPage } from '../pages/Check/CheckInPage/page'
import { CheckDetailPage } from '../pages/Check/CheckDetailPage/page'
import { CheckInEditorPage } from '../pages/Check/CheckInEditorPage/page'
import { QaPage } from '../pages/Qa/QaPage/page'
import { QaDetailPage } from '../pages/Qa/QaDetailPage/page'
import { QuestionEditorPage } from '../pages/Qa/QuestionEditorPage/page'
import { TutorialsPage } from '../pages/TutorialsPage/page'
import { TutorialsEditorPage } from '../pages/TutorialsEditorPage/page'
import { TutorialsManagePage } from '../pages/TutorialsManagePage/page'
import { TutorialsContentEditorPage } from '../pages/TutorialsContentEditorPage/page'
import { PracticePage } from '../pages/Practice/PracticePage/page'
import { PracticeListPage } from '../pages/Practice/PracticeListPage/page'
import { PracticeInterviewTopicPage } from '../pages/Practice/PracticeInterviewTopicPage/page'
import { ContestPage } from '../pages/Contest/ContestPage/page'
import { ContestDetailPage } from '../pages/Contest/ContestDetailPage/page'
import { ClassPage } from '../pages/Class/ClassPage/page'
import { ClassManagePage } from '../pages/Class/ClassManagePage/page'
import { ClassDetailPage } from '../pages/Class/ClassDetailPage/page'
import { AssignmentDetailPage } from '../pages/Class/AssignmentDetailPage/page'
import { InterviewsPage } from '../pages/InterviewsPage/page'
import { DetailPage } from '../pages/DetailPage/page'
import { AuthPage } from '../pages/AuthPage/page'
import { ArticlePage } from '../pages/Article/ArticlePage/page'
import { ArticleEditorPage } from '../pages/Article/ArticleEditorPage/page'
import { ProblemPage } from '../pages/ProblemPage/page'
import { ProfilePage } from '../pages/ProfilePage/page'
import { ProfileEditPage } from '../pages/ProfileEditPage/page'
import { MessagesPage } from '../pages/MessagesPage/page'
import { MembershipPage } from '../pages/MembershipPage/page'
import { SearchPage } from '../pages/SearchPage/page'
import { FeedbackPage } from '../pages/FeedbackPage/page'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<StandaloneLayout />}>
        <Route path="/auth" element={<AuthPage />} />
      </Route>

      <Route element={<AppLayout />}>
        <Route index element={<Navigate replace to="/home" />} />
        <Route path="/contests" element={<ContestPage />} />
        <Route path="/contests/:id" element={<ContestDetailPage />} />
        <Route path="/classes" element={<ClassPage />} />
        <Route path="/classes/manage" element={<RequireCoursePublisher><ClassManagePage /></RequireCoursePublisher>} />
        <Route path="/classes/:id" element={<ClassDetailPage />} />
        <Route path="/classes/:id/assignments/:assignmentId" element={<AssignmentDetailPage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/ai" element={null} />
        <Route path="/feedback" element={<FeedbackPage />} />
        <Route path="/check-in" element={<CheckInPage />} />
        <Route path="/check-in/:id" element={<CheckDetailPage />} />
        <Route path="/check-in/editor" element={<RequireLogin><CheckInEditorPage /></RequireLogin>} />
        <Route path="/qa" element={<QaPage />} />
        <Route path="/qa/question/:id" element={<QaDetailPage />} />
        <Route path="/question/editor" element={<RequireLogin><QuestionEditorPage /></RequireLogin>} />
        <Route path="/question/editor/:id" element={<RequireLogin><QuestionEditorPage /></RequireLogin>} />
        <Route path="/tutorials" element={<TutorialsPage />} />
        <Route path="/tutorials/editor" element={<RequireCoursePublisher><TutorialsEditorPage /></RequireCoursePublisher>} />
        <Route path="/tutorials/editor/:id" element={<RequireCoursePublisher><TutorialsEditorPage /></RequireCoursePublisher>} />
        <Route path="/tutorials/editor/:id/content" element={<RequireCoursePublisher><TutorialsContentEditorPage /></RequireCoursePublisher>} />
        <Route path="/tutorials/manage" element={<RequireCoursePublisher><TutorialsManagePage /></RequireCoursePublisher>} />
        <Route path="/practice" element={<PracticePage />} />
        <Route path="/practice/:sectionId" element={<PracticeListPage />} />
        <Route path="/interviews" element={<InterviewsPage />} />
        <Route path="/interviews/:sectionId" element={<PracticeInterviewTopicPage />} />
        <Route path="/article/:id" element={<ArticlePage />} />
        <Route path="/article/editor" element={<RequireLogin><ArticleEditorPage /></RequireLogin>} />
        <Route path="/article/editor/:id" element={<RequireLogin><ArticleEditorPage /></RequireLogin>} />
        <Route path="/problem/:id" element={<ProblemPage />} />
        <Route path="/detail/:type/:id" element={<DetailPage />} />
        <Route path="/detail/:type/:id/:lessonType/:lessonId" element={<DetailPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/profile/edit" element={<RequireLogin><ProfileEditPage /></RequireLogin>} />
        <Route path="/profile/:userId" element={<ProfilePage />} />
        <Route path="/messages" element={<RequireLogin><MessagesPage /></RequireLogin>} />
        <Route path="/messages/:conversationId" element={<RequireLogin><MessagesPage /></RequireLogin>} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/membership" element={<RequireLogin><MembershipPage /></RequireLogin>} />
      </Route>
    </Routes>
  )
}

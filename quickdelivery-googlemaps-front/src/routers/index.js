/* eslint-disable */
import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
import MyPackages from '../pages/MyPackages.vue';
import HomePage from '../pages/HomePage.vue';
import LandingPage from '../pages/LandingPage.vue';
import LoginPage from '../pages/LoginPage.vue';
import PaymentPage from '../pages/PaymentPage.vue';
import TermsOfUsePage from '../pages/TermsOfUsePage.vue';
import UserSignInPage from '../pages/UserSignInPage.vue';
import PackageConsultationPage from '../pages/PackageConsultationPage.vue';
import PackageTrackingPage from '../pages/PackageTrackingPage.vue';
import PackageTrackingSummaryPage from '../pages/PackageTrackingSummaryPage.vue';
import UserAccountValidationPage from '../pages/UserAccountValidationPage.vue';
import UserAccountPage from '../pages/UserAccountPage.vue';
import UserValidationDetailsPage from '../pages/UserValidationDetailsPage.vue';
import DocumentPage from '../pages/DocumentPage.vue';
import DashboardPage from '../pages/DashboardPage.vue';
import { hasValidAccessToken, redirectToLogin, resolveLandingPathForCurrentUser, userHasAnyRole } from '../config/auth';

const routes = [
      {
        path: '/',
        name: 'landingPage',
        component: LandingPage,
        meta: { public: true }
        },
        {
        path: '/login',
        name: 'loginPage',
        component: LoginPage,
        meta: { public: true, publicOnly: true }
        },
        {
        path: '/register',
        name: 'publicRegisterPage',
        component: UserSignInPage,
        meta: { public: true, publicOnly: true }
        },
        {
        path: '/app',
        name: 'homePage',
        component: HomePage,
        meta: { requiresAuth: true, roles: ['ROLE_LIVREUR', 'ROLE_ADMIN'], keepAlive: true }
        },
        {
        path: '/dashboard/admin',
        name: 'adminDashboardPage',
        component: DashboardPage,
        props: { dashboardType: 'admin' },
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
        },
        {
        path: '/dashboard/courier',
        name: 'courierDashboardPage',
        component: DashboardPage,
        props: { dashboardType: 'courier' },
        meta: { requiresAuth: true, roles: ['ROLE_LIVREUR'] }
        },
        {
        path: '/dashboard/client',
        name: 'clientDashboardPage',
        component: DashboardPage,
        props: { dashboardType: 'client' },
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO'] }
        },
        {
        path: '/terms-of-use',
        name: 'termsOfUsePage',
        component: TermsOfUsePage,
        meta: { public: true }
        },
        {
        path: '/createPackage',
        name: 'createPackage',
        component: PackageCreation,
        meta: { public: true }
        },
        {
        path: '/myPackages',
        name: 'myPackages',
        component: MyPackages,
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
        {
        path: '/paymentPage',
        name: 'paymentPage',
        component:PaymentPage,
        meta: { public: true }
        },
        {
        path: '/userSignInPage',
        name: 'userSignInPageUpdate',
        component:UserSignInPage,
        props: (route) => ({ id: route.query.id, updateToken: route.query.updateToken }),
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
        {
        path: '/userSignInPage',
        name: 'userSignInPage',
        component:UserSignInPage,
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
        {
        path: '/package',
        name: 'PackageConsultationPage',
        component: PackageConsultationPage,
        props: (route) => ({ id: route.query.id, returnTo: route.query.returnTo }),
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
        {
        path: '/packageTracking',
        name: 'PackageTrackingPage',
        component: PackageTrackingPage,
        props: (route) => ({ packageReference: route.query.packageReference }),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
        },
        {
        path: '/packageTrackingSummary',
        name: 'PackageTrackingSummaryPage',
        component: PackageTrackingSummaryPage,
        props: (route) => ({ packageReference: route.query.packageReference, returnTo: route.query.returnTo }),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
        },
        {
        path: '/usersAccountValidation',
        name: 'UserAccountValidationPage',
        component: UserAccountValidationPage,
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
        },
        {
        path: '/userValidationDetails',
        name: 'UserValidationDetailsPage',
        component: UserValidationDetailsPage,
        props: (route) => ({ returnTo: route.query.returnTo }),
        meta: { requiresAuth: true, roles: ['ROLE_ADMIN'] }
        },
        {
        path: '/userAccount',
        name: 'UserAccountPage',
        component: UserAccountPage,
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
        {
        path: '/document',
        name: 'DocumentPage',
        component: DocumentPage,
        props: (route) => ({
          reference: route.query.reference,
          documentType: route.query.documentType,
          returnTo: route.query.returnTo,
        }),
        meta: { requiresAuth: true, roles: ['ROLE_CLIENT', 'ROLE_CLIENT_PRO', 'ROLE_LIVREUR', 'ROLE_ADMIN'] }
        },
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});

router.beforeEach(async (to) => {
    if (to.path === '/userSignInPage' && to.query.updateToken) {
        return true;
    }
    if (to.meta?.publicOnly && hasValidAccessToken()) {
        return resolveLandingPathForCurrentUser();
    }
    if (to.meta?.requiresAuth && !hasValidAccessToken()) {
        await redirectToLogin();
        return false;
    }
    if (to.meta?.roles && !userHasAnyRole(to.meta.roles)) {
        return resolveLandingPathForCurrentUser();
    }
    return true;
});

export default router;

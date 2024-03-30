/* eslint-disable */
import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
import MyPackages from '../pages/MyPackages.vue';
import HomePage from '../pages/HomePage.vue';
import PaymentPage from '../pages/PaymentPage.vue';
import UserSignInPage from '../pages/UserSignInPage.vue';
import PackageConsultationPage from '../pages/PackageConsultationPage.vue';
import PackageTrackingPage from '../pages/PackageTrackingPage.vue';
import UserAccountValidationPage from '../pages/UserAccountValidationPage.vue';

const routes = [
        {
        path: '/',
        name: 'homePage',
        component: HomePage
        },
        {
        path: '/createPackage',
        name: 'createPackage',
        component: PackageCreation
        },
        {
        path: '/myPackages',
        name: 'myPackages',
        component: MyPackages
        },
        {
        path: '/paymentPage',
        name: 'paymentPage',
        component:PaymentPage
        },
        {
        path: '/userSignInPage',
        name: 'userSignInPage',
        component:UserSignInPage
        },
        {
        path: '/package/:id',
        name: 'PackageConsultationPage',
        component: PackageConsultationPage
        },
        {
        path: '/packageTracking/:packageReference',
        name: 'PackageTrackingPage',
        component: PackageTrackingPage
        },
        {
        path: '/usersAccountValidation',
        name: 'UserAccountValidationPage',
        component: UserAccountValidationPage
        },
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});

export default router;
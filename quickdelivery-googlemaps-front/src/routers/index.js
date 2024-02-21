/* eslint-disable */
import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
import MyPackages from '../pages/MyPackages.vue';
import HomePage from '../pages/HomePage.vue';
import PaymentPage from '../pages/PaymentPage.vue';
import UserSignInPage from '../pages/UserSignInPage.vue';

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
        }
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});

export default router;
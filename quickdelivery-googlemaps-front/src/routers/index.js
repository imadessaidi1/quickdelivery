import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
//import PackageCreation from '../pages/ValidationTestPage.vue';
import MyPackages from '../pages/MyPackages.vue';
import HomePage from '../pages/HomePage.vue';

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
        }
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});
export default router;
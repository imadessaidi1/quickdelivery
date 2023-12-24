import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
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
        }
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});
export default router;
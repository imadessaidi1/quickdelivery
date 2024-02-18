/* eslint-disable */
import * as VueRouter from 'vue-router'
import PackageCreation from '../pages/CreatePackagePage.vue';
import MyPackages from '../pages/MyPackages.vue';
import HomePage from '../pages/HomePage.vue';
import PaymentPage from '../pages/PaymentPage.vue';

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
        }
      ];
const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
});

router.beforeResolve((to, from, next) => {
  // If this isn't an initial page load.
  if (to.name) {
    // Start the route progress bar.
    NProgress.start()
  }
  next()
})

router.afterEach((to, from) => {
  // Complete the animation of the route progress bar.
  NProgress.done()
})

export default router;
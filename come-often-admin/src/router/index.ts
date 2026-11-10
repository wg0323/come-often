import { createRouter, createWebHistory } from 'vue-router';
import CouponView from '@/views/CouponView.vue';
import DashboardView from '@/views/DashboardView.vue';
import DishView from '@/views/DishView.vue';
import LoginView from '@/views/LoginView.vue';
import MemberView from '@/views/MemberView.vue';
import OrderView from '@/views/OrderView.vue';
import QueueView from '@/views/QueueView.vue';
import ReservationView from '@/views/ReservationView.vue';
import StatsView from '@/views/StatsView.vue';
import StoreView from '@/views/StoreView.vue';
import { clearToken, getToken } from '@/api/request';
import { useUserStore } from '@/stores/user';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
      meta: { title: '经营看板', requiresAuth: true },
    },
    {
      path: '/dish',
      name: 'dish',
      component: DishView,
      meta: { title: '菜品管理', requiresAuth: true },
    },
    {
      path: '/order',
      name: 'order',
      component: OrderView,
      meta: { title: '订单看板', requiresAuth: true },
    },
    {
      path: '/coupon',
      name: 'coupon',
      component: CouponView,
      meta: { title: '营销管理', requiresAuth: true },
    },
    {
      path: '/member',
      name: 'member',
      component: MemberView,
      meta: { title: '用户管理', requiresAuth: true },
    },
    {
      path: '/reservation',
      name: 'reservation',
      component: ReservationView,
      meta: { title: '预约管理', requiresAuth: true },
    },
    {
      path: '/queue',
      name: 'queue',
      component: QueueView,
      meta: { title: '排队叫号', requiresAuth: true },
    },
    {
      path: '/stats',
      name: 'stats',
      component: StatsView,
      meta: { title: '数据报表', requiresAuth: true },
    },
    {
      path: '/store',
      name: 'store',
      component: StoreView,
      meta: { title: '门店管理', requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录', requiresAuth: false },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth === false) {
    return true;
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  const userStore = useUserStore();
  if (!userStore.info) {
    try {
      await userStore.bootstrap();
    } catch {
      clearToken();
      userStore.$reset();
      return { path: '/login' };
    }
  }
  return true;
});

export default router;

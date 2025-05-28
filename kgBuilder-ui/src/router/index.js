import Vue from "vue";
import VueRouter from "vue-router";
// import Home from "../views/Home.vue"; // Not used, can be removed if Home.vue is not part of this app's routing

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    name: "Home", // Or KGBuilderMain
    component: () => import("../views/kgbuilder/index_v1.vue")
  },
  {
    path: "/builder", // This path now also points to index_v1.vue as the base
    name: "builder",
    component: () => import("../views/kgbuilder/index_v1.vue")
  },
  // {
  //   path: "/kg_v1", // Old path to v1, can be removed or kept if direct access to it under this name is desired
  //   name: "kg_v1",
  //   component: () => import("../views/kgbuilder/index_v1.vue"),
  // },
  {
    path: "/er",
    name: "er",
    component: () => import("../views/erbuilder/index.vue")
  },
  {
    path: "/ds",
    name: "ds",
    component: () => import("../views/datasource/index.vue")
  },
  {
    path: "/icon",
    name: "icon",
    component: () => import("../views/icon/index.vue")
  },
  {
    path: "/about",
    name: "About",
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "about" */ "../views/About.vue")
  }
];

const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes
});

export default router;

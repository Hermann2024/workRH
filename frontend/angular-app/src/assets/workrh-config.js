(function bootstrapWorkRhConfig(windowObject) {
  windowObject.__WORKRH_CONFIG__ = Object.assign(
    {
      apiBaseUrl: "http://localhost:9080",
      frontendBaseUrl: "http://localhost:4200",
      defaultTenantId: "",
      showDemoHints: false,
      defaultLoginEmail: "",
      defaultLoginPassword: ""
    },
    windowObject.__WORKRH_CONFIG__ || {}
  );
})(window);

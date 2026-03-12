const fs = require('fs');
const path = require('path');
const webpack = require('webpack');
const { defineConfig } = require('@vue/cli-service');

const devCertificatePath = path.resolve(__dirname, '../certs/quickdelivery-dev.p12');

module.exports = defineConfig({
  transpileDependencies: true,
  configureWebpack: {
    plugins: [
      new webpack.DefinePlugin({
        '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': true,
      }),
    ],
  },
  devServer: {
    port: 8084,
    host: '0.0.0.0',
    server: {
      type: 'https',
      options: {
        pfx: fs.readFileSync(devCertificatePath),
        passphrase: 'QuickDelivery123@',
      },
    },
    client: false,
    hot: false,
    liveReload: false,
    webSocketServer: false,
  },
});

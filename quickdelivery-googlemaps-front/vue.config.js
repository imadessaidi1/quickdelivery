const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true
});

const webpack = require('webpack');
const fs = require('fs');
module.exports = {
  transpileDependencies: true,
  configureWebpack: {
    plugins: [
      new webpack.DefinePlugin({
        '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': true
      })
    ]
  },
  /*devServer: {
      host: 'quickdelivery.com',
      port: 8080,
      https: {
        key: fs.readFileSync('./src/cert/cle.key'),
        cert: fs.readFileSync('./src/cert/certificat.crt'),
      },
    }*/
};

# This is the source code of OttoPi builtin Web page

It has been created with [Create React App](https://github.com/facebook/create-react-app).

It uses the REST APIs defined in  [ottopi.yaml](../navcomputer/openapi/ottopi.yaml) to communicate 
with the OttoPi box 

The code is split into [controllers](src/controllers) code that communicates with the server and [views](src/views) code responsible to render various screens.

The views are built using [Material-UI](https://material-ui.com) components 

The controllers use the [SwaggerClient](https://www.npmjs.com/package/swagger-client) to invoke REST APIs  

## Available Scripts

In the project directory, you can run:

### `yarn start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.\
You will also see any lint errors in the console.

### `yarn test`

Launches the test runner in the interactive watch mode.\
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `yarn build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.\
Your app is ready to be deployed!

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.


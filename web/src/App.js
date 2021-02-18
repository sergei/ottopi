import './App.css';
import MainScreen from "./MainScreen";
import {createMuiTheme, makeStyles, Paper} from "@material-ui/core";
import { ThemeProvider } from "@material-ui/core/styles";

function App() {

    const darkTheme = createMuiTheme({
        palette: {
            type: 'dark',
        },
    });

    const useStyles = makeStyles({
        paper: {
            width: "100%",
            minHeight: "100vh",
        },
    });
    const classes = useStyles();

    return (
        <ThemeProvider theme={darkTheme}>
            <Paper className={classes.paper}>
                <MainScreen/>
            </Paper>
        </ThemeProvider>
  );
}

export default App;

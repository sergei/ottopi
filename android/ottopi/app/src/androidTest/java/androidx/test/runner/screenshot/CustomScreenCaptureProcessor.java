package androidx.test.runner.screenshot;

import java.io.File;

public class CustomScreenCaptureProcessor extends BasicScreenCaptureProcessor{
    public CustomScreenCaptureProcessor(File screenshotDir){
        super(screenshotDir);
    }
}

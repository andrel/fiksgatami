package no.fiksgatami.tests;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.ViewAsserts;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import no.fiksgatami.R;
import no.fiksgatami.activities.Home;
import no.fiksgatami.components.CameraButton;

import java.util.Locale;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class no.fiksgatami.activities.HomeTest \
 * no.fiksgatami.tests/android.test.InstrumentationTestRunner
 */
public class HomeTest extends ActivityInstrumentationTestCase2<Home> {

    private Home mActivity;
    private CameraButton mCameraButton;
    private Instrumentation mInstrumentation;
    private EditText textSubmissionTitle;
    private Button mGalleryButton;

    public HomeTest() {
        super("no.fiksgatami", Home.class);
    }

    protected void setUp() throws Exception {
        setActivityInitialTouchMode(false);
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();

        mCameraButton = (CameraButton) mActivity.findViewById(R.id.camera_button);
        mGalleryButton = (Button) mActivity.findViewById(R.id.gallery_button);
        textSubmissionTitle = (EditText) mActivity.findViewById(R.id.submission_title);
    }

    public void testPreConditions() throws Exception {
        ViewAsserts.assertOnScreen(mCameraButton.getRootView(), mCameraButton);
        ViewAsserts.assertOnScreen(mGalleryButton.getRootView(), mGalleryButton);
        ViewAsserts.assertOnScreen(textSubmissionTitle.getRootView(), textSubmissionTitle);
    }

    public void testTextFieldContents() throws Exception {
        String expectedTitle = mActivity.getText(R.string.submission_title).toString();
        String actualTitle = textSubmissionTitle.getHint().toString();

        assertEquals("Unexpected text field contents.", expectedTitle, actualTitle);
    }

    public void testTextFieldContentsChanges() throws Exception {
        TouchUtils.tapView(this, textSubmissionTitle);
        sendKeys(KeyEvent.KEYCODE_F);
        sendKeys(KeyEvent.KEYCODE_O);
        sendKeys(KeyEvent.KEYCODE_O);

        String actualTitle = textSubmissionTitle.getText().toString().toUpperCase(Locale.getDefault());

        assertEquals("Unexpected text field contents", "FOO", actualTitle);
    }
}

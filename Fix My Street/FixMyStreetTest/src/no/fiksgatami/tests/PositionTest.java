package no.fiksgatami.tests;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.view.View;
import no.fiksgatami.R;
import no.fiksgatami.activities.Position;

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
public class PositionTest extends ActivityInstrumentationTestCase2<Position> {

    private Instrumentation mInstrumentation;
    private Position mActivity;
    private Intent intent;
    private View mOsmMap;
    private View mButtonBar;
    private View mCancelButton;
    private View mOkButton;

    public PositionTest() {
        super("no.fiksgatami", Position.class);
    }

    protected void setUp() throws Exception {
        setActivityInitialTouchMode(false);
        mInstrumentation = getInstrumentation();
        intent = new Intent();
        intent.putExtra("lat", 51500000d);
        intent.putExtra("long", -150000d);
        setActivityIntent(intent);
        mActivity = getActivity();

        mOsmMap = mActivity.findViewById(R.id.position_osm_map);
        mButtonBar = mActivity.findViewById(R.id.position_button_bar);
        mCancelButton = mActivity.findViewById(R.id.position_cancel_button);
        mOkButton = mActivity.findViewById(R.id.position_ok_button);
    }

    public void testPreConditions() throws Exception {
        ViewAsserts.assertOnScreen(mOsmMap.getRootView(), mOsmMap);
        ViewAsserts.assertOnScreen(mButtonBar.getRootView(), mButtonBar);
        ViewAsserts.assertOnScreen(mOkButton.getRootView(), mOkButton);
        ViewAsserts.assertOnScreen(mCancelButton.getRootView(), mCancelButton);
    }

}

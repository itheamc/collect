package org.odk.collect.android.feature.formentry;

import android.Manifest;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.odk.collect.android.support.CollectTestRule;
import org.odk.collect.android.support.TestDependencies;
import org.odk.collect.android.support.TestRuleChain;
import org.odk.collect.android.support.pages.FormEndPage;
import org.odk.collect.android.support.pages.FormEntryPage;
import org.odk.collect.audiorecorder.recording.AudioRecorderViewModelFactory;
import org.odk.collect.audiorecorder.testsupport.StubAudioRecorderViewModel;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.odk.collect.android.support.FileUtils.copyFileFromAssets;

@RunWith(AndroidJUnit4.class)
public class BackgroundAudioRecordingTest {

    private StubAudioRecorderViewModel stubAudioRecorderViewModel;

    public final TestDependencies testDependencies = new TestDependencies() {
        @Override
        public AudioRecorderViewModelFactory providesAudioRecorderViewModelFactory(Application application) {
            return new AudioRecorderViewModelFactory(application) {
                @Override
                public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                    if (stubAudioRecorderViewModel == null) {
                        try {
                            File stubRecording = File.createTempFile("test", ".m4a");
                            stubRecording.deleteOnExit();

                            copyFileFromAssets("media/test.m4a", stubRecording.getAbsolutePath());
                            stubAudioRecorderViewModel = new StubAudioRecorderViewModel(stubRecording.getAbsolutePath());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return (T) stubAudioRecorderViewModel;
                }
            };
        }
    };

    public final CollectTestRule rule = new CollectTestRule();

    @Rule
    public final RuleChain chain = TestRuleChain.chain(testDependencies)
            .around(GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO))
            .around(rule);

    @Test
    public void whenBackgroundAudioRecordingEnabled_fillingOutForm_recordsAudio() {
        FormEntryPage formEntryPage = rule.mainMenu()
                .enableBackgroundAudioRecording()
                .copyForm("one-question.xml")
                .startBlankForm("One Question");
        assertThat(stubAudioRecorderViewModel.isRecording(), is(true));

        FormEndPage formEndPage = formEntryPage
                .inputText("123")
                .swipeToEndScreen();
        assertThat(stubAudioRecorderViewModel.isRecording(), is(true));

        formEndPage.clickSaveAndExit();
        assertThat(stubAudioRecorderViewModel.isRecording(), is(false));

        assertThat(stubAudioRecorderViewModel.getLastRecording(), notNullValue());
        assertThat(stubAudioRecorderViewModel.getLastRecording().exists(), is(true));
    }
}

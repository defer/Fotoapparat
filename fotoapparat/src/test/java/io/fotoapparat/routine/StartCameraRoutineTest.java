package io.fotoapparat.routine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import io.fotoapparat.error.CameraErrorCallback;
import io.fotoapparat.hardware.CameraDevice;
import io.fotoapparat.hardware.CameraException;
import io.fotoapparat.hardware.orientation.ScreenOrientationProvider;
import io.fotoapparat.parameter.LensPosition;
import io.fotoapparat.parameter.Parameters;
import io.fotoapparat.parameter.provider.InitialParametersProvider;
import io.fotoapparat.parameter.selector.SelectorFunction;
import io.fotoapparat.view.CameraRenderer;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class StartCameraRoutineTest {

    static final int SCREEN_ROTATION_DEGREES = 90;
    static final Parameters INITIAL_PARAMETERS = new Parameters();

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    static final CameraException CAMERA_EXCEPTION = new CameraException("test");

    @Mock
    CameraDevice cameraDevice;
    @Mock
    CameraRenderer cameraRenderer;
    @Mock
    SelectorFunction<LensPosition> lensPositionSelector;
    @Mock
    ScreenOrientationProvider screenOrientationProvider;
    @Mock
    InitialParametersProvider initialParametersProvider;
    @Mock
    CameraErrorCallback cameraErrorCallback;

    @InjectMocks
    StartCameraRoutine testee;

    @Test
    public void routine() throws Exception {
        // Given
        List<LensPosition> availableLensPositions = asList(
                LensPosition.FRONT,
                LensPosition.BACK
        );

        LensPosition preferredLensPosition = LensPosition.FRONT;

        givenLensPositionsAvailable(availableLensPositions);
        givenPositionSelected(preferredLensPosition);
        givenScreenRotation();
        givenInitialParametersAvailable();

        // When
        testee.run();

        // Then
        InOrder inOrder = inOrder(
                cameraDevice,
                cameraRenderer,
                lensPositionSelector
        );

        inOrder.verify(lensPositionSelector).select(availableLensPositions);
        inOrder.verify(cameraDevice).open(preferredLensPosition);
        inOrder.verify(cameraDevice).updateParameters(INITIAL_PARAMETERS);
        inOrder.verify(cameraDevice).setDisplayOrientation(SCREEN_ROTATION_DEGREES);
        inOrder.verify(cameraRenderer).attachCamera(cameraDevice);
        inOrder.verify(cameraDevice).startPreview();

        verifyZeroInteractions(cameraErrorCallback);
    }

    @Test
    public void failedToOpenCamera() throws Exception {
        // Given
        List<LensPosition> availableLensPositions = asList(
                LensPosition.FRONT,
                LensPosition.BACK
        );

        LensPosition preferredLensPosition = LensPosition.FRONT;

        givenLensPositionsAvailable(availableLensPositions);
        givenPositionSelected(preferredLensPosition);

        doThrow(CAMERA_EXCEPTION)
                .when(cameraDevice)
                .open(preferredLensPosition);

        // When
        testee.run();

        // Then
        verify(cameraErrorCallback).onError(CAMERA_EXCEPTION);

        verify(cameraDevice).getAvailableLensPositions();
        verify(cameraDevice).open(preferredLensPosition);
        verifyNoMoreInteractions(cameraDevice);
    }

    private void givenInitialParametersAvailable() {
        given(initialParametersProvider.initialParameters())
                .willReturn(INITIAL_PARAMETERS);
    }

    private void givenScreenRotation() {
        given(screenOrientationProvider.getScreenRotation())
                .willReturn(SCREEN_ROTATION_DEGREES);
    }

    private void givenPositionSelected(LensPosition lensPosition) {
        given(lensPositionSelector.select(ArgumentMatchers.<LensPosition>anyCollection()))
                .willReturn(lensPosition);
    }

    private void givenLensPositionsAvailable(List<LensPosition> lensPositions) {
        given(cameraDevice.getAvailableLensPositions())
                .willReturn(lensPositions);
    }

}
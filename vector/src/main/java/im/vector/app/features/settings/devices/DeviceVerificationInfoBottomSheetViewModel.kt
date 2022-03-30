/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.settings.devices

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo

class DeviceVerificationInfoBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: DeviceVerificationInfoBottomSheetViewState,
                                                                             val session: Session
) : VectorViewModel<DeviceVerificationInfoBottomSheetViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DeviceVerificationInfoBottomSheetViewModel, DeviceVerificationInfoBottomSheetViewState> {
        override fun create(initialState: DeviceVerificationInfoBottomSheetViewState): DeviceVerificationInfoBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<DeviceVerificationInfoBottomSheetViewModel, DeviceVerificationInfoBottomSheetViewState>
    by hiltMavericksViewModelFactory()

    init {
        initState()
        session.flow().liveCrossSigningInfo(session.myUserId)
                .execute {
                    copy(
                            hasAccountCrossSigning = it.invoke()?.getOrNull() != null,
                            accountCrossSigningIsTrusted = it.invoke()?.getOrNull()?.isTrusted() == true
                    )
                }

        session.flow().liveUserCryptoDevices(session.myUserId)
                .map { list ->
                    list.firstOrNull { it.deviceId == initialState.deviceId }
                }
                .execute {
                    copy(
                            cryptoDeviceInfo = it,
                            isMine = it.invoke()?.deviceId == session.sessionParams.deviceId
                    )
                }

        session.flow().liveUserCryptoDevices(session.myUserId)
                .map { it.size }
                .execute {
                    copy(
                            hasOtherSessions = it.invoke() ?: 0 > 1
                    )
                }

        session.flow().liveMyDevicesInfo()
                .map { devices ->
                    devices.firstOrNull { it.deviceId == initialState.deviceId } ?: DeviceInfo(deviceId = initialState.deviceId)
                }
                .execute {
                    copy(deviceInfo = it)
                }
    }

    private fun initState() {
        viewModelScope.launch {
            val hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized()
            val accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified()
            val isRecoverySetup = session.sharedSecretStorageService.isRecoverySetup()
            setState {
                copy(
                        hasAccountCrossSigning = hasAccountCrossSigning,
                        accountCrossSigningIsTrusted = accountCrossSigningIsTrusted,
                        isRecoverySetup = isRecoverySetup
                )
            }
        }
    }

    override fun handle(action: EmptyAction) {
    }
}

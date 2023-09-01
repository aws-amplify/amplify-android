/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.statemachine.codegen.states

import com.amplifyframework.statemachine.State
import com.amplifyframework.statemachine.StateMachineEvent
import com.amplifyframework.statemachine.StateMachineResolver
import com.amplifyframework.statemachine.StateResolution
import com.amplifyframework.statemachine.codegen.actions.SignInActions
import com.amplifyframework.statemachine.codegen.events.SignInEvent

internal sealed class SignInState : State {
    data class NotStarted(val id: String = "") : SignInState()
    data class SigningInWithSRP(override var srpSignInState: SRPSignInState?) : SignInState()
    data class SigningInWithHostedUI(override var hostedUISignInState: HostedUISignInState?) : SignInState()
    data class SigningInWithCustom(override var customSignInState: CustomSignInState?) : SignInState()
    data class SigningInWithSRPCustom(override var srpSignInState: SRPSignInState?) : SignInState()
    data class SigningInViaMigrateAuth(override var migrateSignInState: MigrateSignInState?) : SignInState()
    data class ResolvingDeviceSRP(override var deviceSRPSignInState: DeviceSRPSignInState?) : SignInState()
    data class ResolvingChallenge(override var challengeState: SignInChallengeState?) : SignInState()
    data class ResolvingTOTPSetup(override var setupTOTPState: SetupTOTPState?) : SignInState()
    data class ConfirmingDevice(val id: String = "") : SignInState()
    data class Done(val id: String = "") : SignInState()
    data class Error(val exception: Exception) : SignInState()
    data class SignedIn(val id: String = "") : SignInState()

    open var srpSignInState: SRPSignInState? = SRPSignInState.NotStarted()
    open var challengeState: SignInChallengeState? = SignInChallengeState.NotStarted()
    open var customSignInState: CustomSignInState? = CustomSignInState.NotStarted()
    open var migrateSignInState: MigrateSignInState? = MigrateSignInState.NotStarted()
    open var hostedUISignInState: HostedUISignInState? = HostedUISignInState.NotStarted()
    open var deviceSRPSignInState: DeviceSRPSignInState? = DeviceSRPSignInState.NotStarted()
    open var setupTOTPState: SetupTOTPState? = SetupTOTPState.NotStarted()

    class Resolver(
        private val srpSignInResolver: StateMachineResolver<SRPSignInState>,
        private val customSignInResolver: StateMachineResolver<CustomSignInState>,
        private val migrationSignInResolver: StateMachineResolver<MigrateSignInState>,
        private val challengeResolver: StateMachineResolver<SignInChallengeState>,
        private val hostedUISignInResolver: StateMachineResolver<HostedUISignInState>,
        private val deviceSRPSignInResolver: StateMachineResolver<DeviceSRPSignInState>,
        private val setupTOTPResolver: StateMachineResolver<SetupTOTPState>,
        private val signInActions: SignInActions
    ) :
        StateMachineResolver<SignInState> {
        override val defaultState = NotStarted()

        private fun asSignInEvent(event: StateMachineEvent): SignInEvent.EventType? {
            return (event as? SignInEvent)?.eventType
        }

        override fun resolve(oldState: SignInState, event: StateMachineEvent): StateResolution<SignInState> {
            val resolution = resolveSignInEvent(oldState, event)
            val actions = resolution.actions.toMutableList()
            val builder = Builder(resolution.newState)

            oldState.srpSignInState?.let { srpSignInResolver.resolve(it, event) }?.let {
                builder.srpSignInState = it.newState
                actions += it.actions
            }

            oldState.challengeState?.let { challengeResolver.resolve(it, event) }?.let {
                builder.challengeState = it.newState
                actions += it.actions
            }

            oldState.migrateSignInState?.let { migrationSignInResolver.resolve(it, event) }?.let {
                builder.migrateSignInState = it.newState
                actions += it.actions
            }

            oldState.hostedUISignInState?.let { hostedUISignInResolver.resolve(it, event) }?.let {
                builder.hostedUISignInState = it.newState
                actions += it.actions
            }

            oldState.customSignInState?.let { customSignInResolver.resolve(it, event) }?.let {
                builder.customSignInState = it.newState
                actions += it.actions
            }

            oldState.deviceSRPSignInState?.let { deviceSRPSignInResolver.resolve(it, event) }?.let {
                builder.deviceSRPSignInState = it.newState
                actions += it.actions
            }

            oldState.setupTOTPState?.let { setupTOTPResolver.resolve(it, event) }?.let {
                builder.setupTOTPState = it.newState
                actions += it.actions
            }
            return StateResolution(builder.build(), actions)
        }

        private fun resolveSignInEvent(
            oldState: SignInState,
            event: StateMachineEvent
        ): StateResolution<SignInState> {
            val signInEvent = asSignInEvent(event)
            val defaultResolution = StateResolution(oldState)
            return when (oldState) {
                is NotStarted -> when (signInEvent) {
                    is SignInEvent.EventType.InitiateSignInWithSRP -> StateResolution(
                        SigningInWithSRP(oldState.srpSignInState),
                        listOf(signInActions.startSRPAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.InitiateSignInWithCustom -> StateResolution(
                        SigningInWithCustom(oldState.customSignInState),
                        listOf(signInActions.startCustomAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.InitiateHostedUISignIn -> StateResolution(
                        SigningInWithHostedUI(HostedUISignInState.NotStarted()),
                        listOf(signInActions.startHostedUIAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.InitiateMigrateAuth -> StateResolution(
                        SigningInViaMigrateAuth(MigrateSignInState.NotStarted()),
                        listOf(signInActions.startMigrationAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.InitiateCustomSignInWithSRP -> StateResolution(
                        SigningInWithSRPCustom(oldState.srpSignInState),
                        listOf(signInActions.startCustomAuthWithSRPAction(signInEvent))
                    )

                    else -> defaultResolution
                }

                is SigningInWithSRP, is SigningInWithCustom, is SigningInViaMigrateAuth,
                is SigningInWithSRPCustom
                -> when (signInEvent) {
                    is SignInEvent.EventType.ReceivedChallenge -> {
                        val action = signInActions.initResolveChallenge(signInEvent)
                        StateResolution(ResolvingChallenge(oldState.challengeState), listOf(action))
                    }

                    is SignInEvent.EventType.InitiateSignInWithDeviceSRP -> StateResolution(
                        ResolvingDeviceSRP(DeviceSRPSignInState.NotStarted()),
                        listOf(signInActions.startDeviceSRPAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.ConfirmDevice -> {
                        val action = signInActions.confirmDevice(signInEvent)
                        StateResolution(ConfirmingDevice(), listOf(action))
                    }

                    is SignInEvent.EventType.InitiateTOTPSetup -> StateResolution(
                        ResolvingTOTPSetup(oldState.setupTOTPState),
                        listOf(signInActions.initiateTOTPSetupAction(signInEvent))
                    )

                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception))
                    else -> defaultResolution
                }

                is ResolvingChallenge -> when (signInEvent) {
                    is SignInEvent.EventType.ConfirmDevice -> {
                        val action = signInActions.confirmDevice(signInEvent)
                        StateResolution(ConfirmingDevice(), listOf(action))
                    }

                    is SignInEvent.EventType.ReceivedChallenge -> {
                        val action = signInActions.initResolveChallenge(signInEvent)
                        StateResolution(ResolvingChallenge(oldState.challengeState), listOf(action))
                    }

                    is SignInEvent.EventType.InitiateTOTPSetup -> StateResolution(
                        ResolvingTOTPSetup(oldState.setupTOTPState),
                        listOf(signInActions.initiateTOTPSetupAction(signInEvent))
                    )

                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception))
                    else -> defaultResolution
                }

                is ResolvingTOTPSetup -> when (signInEvent) {
                    is SignInEvent.EventType.ReceivedChallenge -> {
                        val action = signInActions.initResolveChallenge(signInEvent)
                        StateResolution(ResolvingChallenge(oldState.challengeState), listOf(action))
                    }

                    is SignInEvent.EventType.ConfirmDevice -> {
                        val action = signInActions.confirmDevice(signInEvent)
                        StateResolution(ConfirmingDevice(), listOf(action))
                    }

                    is SignInEvent.EventType.InitiateSignInWithDeviceSRP -> StateResolution(
                        ResolvingDeviceSRP(DeviceSRPSignInState.NotStarted()),
                        listOf(signInActions.startDeviceSRPAuthAction(signInEvent))
                    )

                    is SignInEvent.EventType.FinalizeSignIn -> {
                        StateResolution(SignedIn())
                    }

                    else -> defaultResolution
                }

                is ResolvingDeviceSRP -> when (signInEvent) {
                    is SignInEvent.EventType.ReceivedChallenge -> {
                        val action = signInActions.initResolveChallenge(signInEvent)
                        StateResolution(ResolvingChallenge(oldState.challengeState), listOf(action))
                    }

                    is SignInEvent.EventType.InitiateTOTPSetup -> StateResolution(
                        ResolvingTOTPSetup(SetupTOTPState.NotStarted()),
                        listOf(signInActions.initiateTOTPSetupAction(signInEvent))
                    )

                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception))

                    else -> defaultResolution
                }

                is ConfirmingDevice -> when (signInEvent) {
                    is SignInEvent.EventType.FinalizeSignIn -> {
                        StateResolution(SignedIn())
                    }

                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception))
                    else -> defaultResolution
                }

                is SigningInWithHostedUI -> when (signInEvent) {
                    is SignInEvent.EventType.SignedIn -> StateResolution(Done())
                    is SignInEvent.EventType.ThrowError -> StateResolution(Error(signInEvent.exception))
                    else -> defaultResolution
                }

                else -> defaultResolution
            }
        }
    }

    class Builder(private val signInState: SignInState) :
        com.amplifyframework.statemachine.Builder<SignInState> {
        var srpSignInState: SRPSignInState? = null
        var challengeState: SignInChallengeState? = null
        var customSignInState: CustomSignInState? = null
        var migrateSignInState: MigrateSignInState? = null
        var hostedUISignInState: HostedUISignInState? = null
        var deviceSRPSignInState: DeviceSRPSignInState? = null
        var setupTOTPState: SetupTOTPState? = null

        override fun build(): SignInState = when (signInState) {
            is SigningInWithSRP -> SigningInWithSRP(srpSignInState)
            is ResolvingChallenge -> ResolvingChallenge(challengeState)
            is SigningInViaMigrateAuth -> SigningInViaMigrateAuth(migrateSignInState)
            is SigningInWithCustom -> SigningInWithCustom(customSignInState)
            is SigningInWithHostedUI -> SigningInWithHostedUI(hostedUISignInState)
            is SigningInWithSRPCustom -> SigningInWithSRPCustom(srpSignInState)
            is ResolvingDeviceSRP -> ResolvingDeviceSRP(deviceSRPSignInState)
            is ResolvingTOTPSetup -> ResolvingTOTPSetup(setupTOTPState)
            else -> signInState
        }
    }
}

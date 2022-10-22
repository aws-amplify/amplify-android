package com.amplifyframework.auth.cognito.featuretest

/**
 * List of APIs supported by Auth.
 * Note that case of ENUMs are not capitalized so as to serialze it into proper case
 * something like `resetPassword` instead of `RESET_PASSWORD`
 */
enum class AuthAPI {
    confirmResetPassword,
    confirmSignIn,
    confirmSignUp,
    confirmUserAttribute,
    deleteUser,
    fetchAuthSession,
    fetchDevices,
    fetchUserAttributes,
    forgetDevice,
    getCurrentUser,
    handleWebUISignInResponse,
    rememberDevice,
    resendSignUpCode,
    resendUserAttributeConfirmationCode,
    resetPassword,
    signIn,
    signInWithSocialWebUI,
    signInWithWebUI,
    signOut,
    signUp,
    updatePassword,
    updateUserAttribute,
    updateUserAttributes
}

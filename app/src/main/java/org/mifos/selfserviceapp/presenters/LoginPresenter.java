package org.mifos.selfserviceapp.presenters;

import android.content.Context;
import android.content.res.Resources;

import org.mifos.selfserviceapp.R;
import org.mifos.selfserviceapp.api.DataManager;
import org.mifos.selfserviceapp.data.User;
import org.mifos.selfserviceapp.injection.ActivityContext;
import org.mifos.selfserviceapp.presenters.base.BasePresenter;
import org.mifos.selfserviceapp.ui.views.LoginView;
import org.mifos.selfserviceapp.utils.Constants;
import org.mifos.selfserviceapp.utils.PrefManager;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Vishwajeet
 * @since 05/06/16
 */
public class LoginPresenter extends BasePresenter<LoginView> {

    private DataManager dataManager;

    /**
     * Initialises the LoginPresenter by automatically injecting an instance of
     * {@link DataManager} and {@link Context}.
     *
     * @param dataManager DataManager class that provides access to the data
     *                    via the API.
     * @param context     Context of the view attached to the presenter. In this case
     *                    it is that of an {@link android.support.v7.app.AppCompatActivity}
     */
    @Inject
    public LoginPresenter(DataManager dataManager, @ActivityContext Context context) {
        super(context);
        this.dataManager = dataManager;
    }

    /**
     * This method validates the username and password entered by the user
     * and reports any errors that might exists in any of the inputs.
     * If there are no errors, then we attempt to authenticate the user from
     * the server and then persist the authentication data if we successfully
     * authenticate the credentials or notify the view about any errors.
     *
     * @param username Username of the user trying to login.
     * @param password Password of the user trying to login.
     */
    public void login(final String username, final String password) {

        final Resources resources = context.getResources();

        if (username == null || username.trim().isEmpty()) {
            showEmptyInputError(resources.getString(R.string.username));
            return;
        } else if (username.length() < 5) {
            showMinimumInputLengthNotAchievedError(resources.getString(R.string.username),
                    resources.getInteger(R.integer.username_minimum_length));
            return;
        } else if (username.contains(" ")) {
            getMvpView().showInputValidationError(context.getString(
                    R.string.error_validation_cannot_contain_spaces,
                    R.string.username, R.string.not_contain_username));
            return;
        } else if (password == null || password.trim().isEmpty()) {
            showEmptyInputError(context.getString(R.string.password));
            return;
        } else if (password.length() < 6) {
            showMinimumInputLengthNotAchievedError(resources.getString(R.string.password),
                    resources.getInteger(R.integer.password_minimum_length));
            return;
        }

        Call<User> call = dataManager.login(username, password);
        getMvpView().showProgress();
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Response<User> response) {
                getMvpView().hideProgress();

                if (response.code() == 200) {
                    final User user = response.body();
                    if (user != null) {
                        final String userName = user.getUserName();
                        getMvpView().onLoginSuccess(userName);

                        final long userID = user.getUserId();
                        final String authToken = Constants.BASIC +
                                user.getBase64EncodedAuthenticationKey();
                        saveAuthenticationTokenForSession(userID, authToken);
                    }
                } else if (response.code() == 401) {
                    getMvpView().onLoginError(context.getString(R.string.error_unauthorised));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                getMvpView().hideProgress();
                getMvpView().onLoginError(context.getString(R.string.error_message_server));
            }
        });
    }

    /**
     * Save the authentication token from the server and the user ID.
     * The authentication token would be used for accessing the authenticated
     * APIs.
     *
     * @param userID    - The userID of the user to be saved.
     * @param authToken - The authentication token to be saved.
     */
    private void saveAuthenticationTokenForSession(long userID, String authToken) {

        final PrefManager prefManager = dataManager.getPrefManager();
        prefManager.setUserId(userID);
        prefManager.saveToken(authToken);
        dataManager.authenticateApiManager();

    }

    /**
     * Notifies the view about an empty input in the given field name.
     *
     * @param fieldName Field name of the input that was empty
     */
    private void showEmptyInputError(String fieldName) {
        getMvpView().showInputValidationError(context.getString(
                R.string.error_validation_blank, fieldName));
    }

    /**
     * Notifies the view that the user has not entered the minimum number of
     * characters the input requires.
     *
     * @param fieldName     Field name of the input.
     * @param minimumLength Minimum number of characters the field requires.
     */
    private void showMinimumInputLengthNotAchievedError(String fieldName, int minimumLength) {
        getMvpView().showInputValidationError(context.getString(
                R.string.error_validation_minimum_chars,
                fieldName, minimumLength));
    }

}

package booking.app.telegram.service.message.handlers;

import booking.app.controller.AuthenticationController;
import booking.app.dto.user.UserLoginRequestDto;
import booking.app.model.User;
import booking.app.telegram.service.message.MessageService;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@RequiredArgsConstructor
public class LoginMessageServiceImpl implements MessageService {
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String PATTERN_OF_PASSWORD =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    private static final String EMAIL = "EMAIL";
    private static final String PASSWORD = "PASSWORD";
    private static final String SIGN_IN = "SIGN IN";

    private final AuthenticationController authenticationController;

    private UserLoginRequestDto loginRequestDto = new UserLoginRequestDto();
    private String jwtToken;

    @Override
    public SendMessage getResponseMessage(Message message, String jwtToken, User user) {
        checkAndSetEmailAndPassword(message);
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());

        if (loginRequestDto.getEmail() != null && loginRequestDto.getPassword() != null) {
            message.setText("SIGN IN");
        }

        return switch (message.getText()) {
            case EMAIL -> createSendMessage(response, null, "Enter your email: ");
            case PASSWORD -> createSendMessage(response, null, "Enter your password: ");
            case SIGN_IN -> signInMessage(response);
            default -> createSendMessage(
                    response,
                    loginKeyboard(),
                    "Go to the E-mail or password tab to enter the data."
            );
        };
    }

    @Override
    public String getJwtToken() {
        return jwtToken;
    }

    private SendMessage createSendMessage(
            SendMessage response, ReplyKeyboardMarkup markup, String text
    ) {
        if (markup != null) {
            response.setReplyMarkup(markup);
        }

        if (text != null && !text.isEmpty()) {
            response.setText(text);
        }

        return response;
    }

    private SendMessage signInMessage(SendMessage response) {
        response.setReplyMarkup(getSignInKeyboard());
        response.setText("Check your details: " + System.lineSeparator()
                + "email: " + loginRequestDto.getEmail() + System.lineSeparator()
                + "password: " + loginRequestDto.getPassword() + System.lineSeparator()
                + "and Sign in.");
        try {
            jwtToken = authenticationController.login(loginRequestDto).token();
        } catch (AuthenticationException e) {
            loginRequestDto.setPassword(null);
            loginRequestDto.setEmail(null);
            return createSendMessage(
                    response,
                    loginKeyboard(),
                    e.getMessage() + System.lineSeparator()
                            + "Re-enter your email and password"
            );
        }
        return response;
    }

    private ReplyKeyboardMarkup loginKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add(EMAIL);
        row.add(PASSWORD);
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private ReplyKeyboardMarkup getSignInKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add(SIGN_IN);
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private void checkAndSetEmailAndPassword(Message message) {
        if (Pattern.compile(EMAIL_PATTERN).matcher(message.getText()).matches()) {
            loginRequestDto.setEmail(message.getText().trim());
        }

        if (Pattern.compile(PATTERN_OF_PASSWORD).matcher(message.getText()).matches()) {
            loginRequestDto.setPassword(message.getText().trim());
        }
    }
}

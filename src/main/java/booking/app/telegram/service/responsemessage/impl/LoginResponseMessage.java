package booking.app.telegram.service.responsemessage.impl;

import booking.app.controller.AuthenticationController;
import booking.app.dto.user.UserLoginRequestDto;
import booking.app.telegram.service.responsemessage.ResponseMessage;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Service
@RequiredArgsConstructor
public class LoginResponseMessage implements ResponseMessage {
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String PATTERN_OF_PASSWORD =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    private final AuthenticationController authenticationController;

    private UserLoginRequestDto loginRequestDto = new UserLoginRequestDto();

    @Getter
    private String jwtToken;

    @Override
    public SendMessage getResponseMessage(final Message message) {
        if (Pattern.compile(EMAIL_PATTERN).matcher(message.getText()).matches()) {
            loginRequestDto.setEmail(message.getText().trim());
        }

        if (Pattern.compile(PATTERN_OF_PASSWORD).matcher(message.getText()).matches()) {
            loginRequestDto.setPassword(message.getText().trim());
        }

        if (loginRequestDto.getEmail() != null && loginRequestDto.getPassword() != null) {
            message.setText("SIGN IN");
        }

        return switch (message.getText()) {
            case "EMAIL" -> createSendMessage(message, "Enter your email: ", null);
            case "PASSWORD" -> createSendMessage(message, "Enter your password: ", null);
            case "SIGN IN" -> signInMessage(message);
            default -> createSendMessage(
                    message,
                    "Go to the E-mail or password tab to enter the data.",
                    loginKeyboard()
            );
        };
    }

    @Override
    public String getJwtToken() {
        return jwtToken;
    }

    private SendMessage createSendMessage(
            Message message, String text, ReplyKeyboardMarkup markup
    ) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        response.setText(text);
        if (markup != null) {
            response.setReplyMarkup(markup);
        }
        return response;
    }

    private SendMessage signInMessage(Message message) {
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
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
                    message,
                    e.getMessage() + System.lineSeparator()
                            + "Re-enter your email and password",
                    loginKeyboard()
            );
        }
        return response;
    }

    private ReplyKeyboardMarkup loginKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add("EMAIL");
        row.add("PASSWORD");
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private ReplyKeyboardMarkup getSignInKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        row.add("SIGN IN");
        markup.setKeyboard(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }
}

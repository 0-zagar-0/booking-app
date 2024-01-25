package booking.app.telegram;

import booking.app.controller.AuthenticationController;
import booking.app.exception.DataProcessingException;
import booking.app.model.User;
import booking.app.security.JwtUtil;
import booking.app.service.user.UserService;
import booking.app.telegram.model.BotChat;
import booking.app.telegram.model.Separator;
import booking.app.telegram.service.TelegramService;
import booking.app.telegram.service.message.MessageService;
import booking.app.telegram.service.message.handlers.LoginMessageServiceImpl;
import booking.app.telegram.service.message.handlers.MainMenuMessageServiceImpl;
import booking.app.telegram.service.message.handlers.StartMessageServiceImpl;
import booking.app.telegram.service.message.handlers.UserMessageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class BookingBot extends TelegramLongPollingBot {
    private static final String START = "/start";
    private static final String LOGIN = "LOGIN";
    private static final String SIGN_IN = "SIGN IN";
    private static final String MAIN_MENU = "MAIN MENU";
    private static final String USER = "USER";
    private static final String BACK_TO_START_MENU = "BACK TO START MENU";

    private final AuthenticationController authenticationController;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final TelegramService telegramService;

    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.name}")
    private String botName;
    private MessageService responseMessage;
    private String jwtToken;
    private User user;

    @Override
    public void onUpdateReceived(final Update update) {
        final Message message = update.getMessage();

        try {
            execute(getResponseMessage(message));
            if (responseMessage instanceof LoginMessageServiceImpl) {
                jwtToken = responseMessage.getJwtToken();

                if (jwtToken != null && !jwtToken.isEmpty()) {
                    user = userService.getByEmail(jwtUtil.getUserName(jwtToken));
                }
            }
        } catch (TelegramApiException e) {
            throw new DataProcessingException("Telegram exception: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public void handleIncomingMessage(String objectString) {
        for (BotChat bot : telegramService.findAll()) {
            SendMessage response = new SendMessage();
            response.setChatId(bot.getChatId());
            response.setText(parseDataAndGenerateResponse(objectString));
            sendResponse(response);
        }
    }

    private void sendResponse(SendMessage response) {
        try {
            execute(response);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("Forbidden")) {
                return;
            }
            throw new RuntimeException(e);
        }
    }

    private String parseDataAndGenerateResponse(String data) {
        int indexStartPositionResponseData =
                data.indexOf(Separator.L_SQUARE_BRACKET.getSeparator()) + 1;
        String newData = data.substring(indexStartPositionResponseData, data.length() - 1);
        String[] dataValue = newData.split(Separator.COMMA.getSeparator());

        int indexDescriptionMessage = data.indexOf(Separator.PIPE.getSeparator());
        String descriptionMessage = data.substring(0, indexDescriptionMessage);

        StringBuilder correctData = new StringBuilder();
        correctData.append(descriptionMessage).append(System.lineSeparator());

        for (int i = 0; i < dataValue.length; i++) {
            if (dataValue[i].contains(Separator.EQUALS.getSeparator())) {
                String replaceString = dataValue[i]
                        .replaceFirst(String.valueOf(dataValue[i].trim().charAt(0)),
                                String.valueOf(dataValue[i].trim().charAt(0)).toUpperCase()).trim();
                correctData.append(replaceString.replace(Separator.EQUALS.getSeparator(),
                        Separator.COLON.getSeparator() + Separator.SPACE.getSeparator()));

                for (int j = i + 1; j < dataValue.length; j++) {
                    if (!dataValue[j].contains(Separator.EQUALS.getSeparator())) {
                        correctData.append(Separator.COMMA.getSeparator()).append(dataValue[j]);
                        i = j;
                    } else {
                        break;
                    }
                }
                correctData.append(System.lineSeparator());
            }
        }
        return correctData.toString();
    }

    private SendMessage getResponseMessage(Message message) {
        initializeResponseMessage(message);
        return responseMessage.getResponseMessage(message, jwtToken, user);
    }

    private void initializeResponseMessage(Message message) {
        switch (message.getText()) {
            case START -> responseMessage = new StartMessageServiceImpl(telegramService);
            case LOGIN -> responseMessage = new LoginMessageServiceImpl(authenticationController);
            case SIGN_IN -> responseMessage = new MainMenuMessageServiceImpl();
            case MAIN_MENU -> responseMessage = new MainMenuMessageServiceImpl();
            case USER -> responseMessage = new UserMessageServiceImpl(userService, objectMapper);
            case BACK_TO_START_MENU -> responseMessage =
                    new StartMessageServiceImpl(telegramService);
            default -> {
                return;
            }
        }
    }
}

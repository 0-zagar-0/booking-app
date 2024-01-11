package booking.app.telegram.service.message.handlers;

import booking.app.dto.user.UserResponseDto;
import booking.app.dto.user.UserUpdateProfileInformationDto;
import booking.app.dto.user.UserUpdateRoleDto;
import booking.app.exception.TelegramException;
import booking.app.model.User;
import booking.app.service.user.UserService;
import booking.app.telegram.model.Separator;
import booking.app.telegram.service.message.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Getter
@Setter
@RequiredArgsConstructor
public class UserMessageServiceImpl implements MessageService {
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_HEADER = "Bearer ";
    private static final String EMAIL = "Email";
    private static final String PASSWORD = "Password";
    private static final String FIRST_NAME = "First name";
    private static final String LAST_NAME = "Last name";
    private static final String GET_USER_PROFILE = "GET USER PROFILE";
    private static final String UPDATE_USER_PROFILE = "UPDATE USER PROFILE";
    private static final String UPDATE_USER_ROLE = "UPDATE USER ROLE";
    private static final String MAIN_MENU = "MAIN MENU";
    private static final String USER_ID = "User id";
    private static final String USER_ROLE = "User role";
    private static final String USER_ME_URL = "http://localhost:8080/api/users/me";

    private final UserService userService;
    private final ObjectMapper objectMapper;

    private HttpClient client;
    private User newUser;

    @Override
    public SendMessage getResponseMessage(Message message, String jwtToken, User user) {
        newUser = user;
        SendMessage response = new SendMessage();
        response.setChatId(message.getChatId());
        client = HttpClient.newHttpClient();

        try {
            checkMessageTextAndCallUpdateMethods(message, jwtToken);
        } catch (TelegramException e) {
            response.setText(e.getMessage());
            return response;
        }

        return switch (message.getText()) {
            case GET_USER_PROFILE -> getUserProfile(
                    response,
                    USER_ME_URL,
                    jwtToken
            );
            case UPDATE_USER_PROFILE -> getUpdateUserProfile(response);
            case UPDATE_USER_ROLE -> getUpdateUserRole(response);
            default -> getUserMenu(response);
        };
    }

    private SendMessage getUserMenu(SendMessage response) {
        String message = "Choose endpoint";

        if (response.getText() != null) {
            message = response.getText();
        }

        return createSendMessage(response, getUserKeyboard(), message);
    }

    private SendMessage getUserProfile(SendMessage response, String url, String jwtToken) {
        try {
            HttpResponse<String> httpResponse = client.send(
                    getGetHttpRequest(jwtToken, url),
                    HttpResponse.BodyHandlers.ofString()
            );

            UserResponseDto userResponseDto =
                    objectMapper.readValue(httpResponse.body(), UserResponseDto.class);

            return createSendMessage(
                    response,
                    getUserKeyboard(),
                    generateResponse(userResponseDto.toString())
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private SendMessage getUpdateUserProfile(SendMessage response) {
        String text = new StringBuilder(
                "Copy this form, paste it yourself and enter the necessary parameters to update: ")
                .append(System.lineSeparator()).append(System.lineSeparator())
                .append(EMAIL).append(Separator.COLON.getSeparator()).append(System.lineSeparator())
                .append(Separator.COMMA.getSeparator()).append(PASSWORD)
                .append(Separator.COLON.getSeparator()).append(System.lineSeparator())
                .append(Separator.COMMA.getSeparator()).append(FIRST_NAME)
                .append(Separator.COLON.getSeparator()).append(System.lineSeparator())
                .append(Separator.COMMA.getSeparator()).append(LAST_NAME)
                .append(Separator.COLON.getSeparator())
                .toString();

        return createSendMessage(response, getUpdateKeyboard(), text);
    }

    private SendMessage getUpdateUserRole(SendMessage response) {
        StringBuilder messageBuilder = new StringBuilder(
                "Copy this form, paste it yourself and enter the necessary parameters to update.")
                .append(System.lineSeparator())
                .append("There are ")
                .append(User.Role.values().length)
                .append(" roles, enter one of your choice: ");

        int counter = User.Role.values().length;
        for (User.Role role : User.Role.values()) {
            if (counter == 1) {
                messageBuilder.append(role.name());
                break;
            }

            messageBuilder.append(role.name())
                    .append(Separator.COMMA.getSeparator())
                    .append(Separator.SPACE.getSeparator());
            counter--;
        }

        messageBuilder.append(System.lineSeparator()).append(System.lineSeparator())
                .append(USER_ID)
                .append(Separator.COLON.getSeparator())
                .append(System.lineSeparator())
                .append(Separator.COMMA.getSeparator())
                .append(USER_ROLE)
                .append(Separator.COLON.getSeparator())
                .toString();

        String message = messageBuilder.toString();

        return createSendMessage(response, getUpdateKeyboard(), message);
    }

    private ReplyKeyboardMarkup getUserKeyboard() {
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow getRow = new KeyboardRow();
        getRow.add(GET_USER_PROFILE);
        rows.add(getRow);

        KeyboardRow updateRow = new KeyboardRow();
        updateRow.add(UPDATE_USER_PROFILE);
        rows.add(updateRow);

        if (newUser != null && newUser.getRole().equals(User.Role.ROLE_MANAGER)) {
            updateRow.add(UPDATE_USER_ROLE);
        }

        KeyboardRow mainMenuRow = new KeyboardRow();
        mainMenuRow.add(MAIN_MENU);
        rows.add(mainMenuRow);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(rows);
        markup.setResizeKeyboard(true);
        return markup;
    }

    private ReplyKeyboardMarkup getUpdateKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row2 = new KeyboardRow();
        row2.add(MAIN_MENU);
        markup.setKeyboard(List.of(row2));
        markup.setResizeKeyboard(true);
        return markup;
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

    private HttpRequest getGetHttpRequest(String jwtToken, String url) {
        return HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header(AUTHORIZATION_HEADER, BEARER_HEADER + jwtToken)
                .build();
    }

    private HttpRequest getPutHttpRequest(
            String jwtToken, String url, Object updateUser
    ) {
        try {
            String json = objectMapper.writeValueAsString(updateUser);

            return HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(url))
                    .header(AUTHORIZATION_HEADER, BEARER_HEADER + jwtToken)
                    .header("Content-Type", "application/json")
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateResponse(String data) {
        int firstIndex = data.indexOf("[") + 1;
        String newData = data.substring(firstIndex, data.length() - 1);
        String[] dataValue = newData.split(",");
        StringBuilder builder = new StringBuilder();

        for (String value : dataValue) {
            builder.append(value).append(System.lineSeparator());
        }

        return builder.toString();
    }

    private void updateUserProfile(
            Message message,
            String url,
            String jwtToken
    ) {

        UserUpdateProfileInformationDto updateUser = parseUpdateDataAndUpdateUser(message);
        try {
            client.send(
                    getPutHttpRequest(jwtToken, url, updateUser),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateUserRole(
            Message message,
            String jwtToken
    ) {
        List<String> updateRoleData = parseUpdateRoleData(message);
        UserUpdateRoleDto updateRoleDto = null;
        String url = null;

        if (updateRoleData.size() > 1) {
            for (User.Role value : User.Role.values()) {
                if (updateRoleData.get(1).equals(value.name())) {
                    updateRoleDto = new UserUpdateRoleDto(value);
                }
            }
            url = "http://localhost:8080/api/users/" + updateRoleData.get(0) + "/role";
        }

        try {
            client.send(
                    getPutHttpRequest(jwtToken, url, updateRoleDto),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (IOException | InterruptedException e) {
            throw new TelegramException("An error occurred while updating the user role.");
        }
    }

    private void checkMessageTextAndCallUpdateMethods(Message message, String jwtToken) {
        String text = message.getText();

        if (text.contains(USER_ID) && text.contains(USER_ROLE)) {
            updateUserRole(message, jwtToken);
        }

        if (text.contains(EMAIL) && text.contains(PASSWORD)
                && text.contains(FIRST_NAME) && text.contains(LAST_NAME)) {
            updateUserProfile(message, USER_ME_URL, jwtToken);
        }
    }

    private UserUpdateProfileInformationDto parseUpdateDataAndUpdateUser(Message message) {
        String text = message.getText();

        String email = newUser.getEmail();
        String password = newUser.getPassword();
        String firstName = newUser.getFirstName();
        String lastName = newUser.getLastName();

        if (text.contains(EMAIL)
                && text.contains(PASSWORD)
                && text.contains(FIRST_NAME)
                && text.contains(LAST_NAME)
        ) {
            String[] data = text.split(Separator.COMMA.getSeparator());

            for (String value : data) {
                String[] dataValue = value.split(Separator.COLON.getSeparator());

                if (dataValue.length > 1 && dataValue[0].equals(EMAIL)) {
                    if (dataValue[1].trim().isEmpty()) {
                        continue;
                    }

                    if (!Pattern.compile(EMAIL_PATTERN).matcher(dataValue[1].trim()).matches()) {
                        throw new TelegramException("The email was entered incorrectly");
                    }

                    email = dataValue[1].trim();
                    newUser.setEmail(email);
                }

                if (dataValue.length > 1 && dataValue[0].equals(PASSWORD)) {
                    if (dataValue[1].trim().isEmpty()) {
                        continue;
                    }

                    if (!Pattern.compile(PASSWORD_PATTERN).matcher(dataValue[1].trim()).matches()) {
                        throw new TelegramException("The password was entered incorrectly");
                    }

                    password = dataValue[1].trim();
                    newUser.setPassword(password);
                }

                if (dataValue.length > 1 && !dataValue[1].trim().isEmpty()
                        && dataValue[0].trim().equals(FIRST_NAME)) {
                    firstName = dataValue[1].trim();
                    newUser.setFirstName(firstName);
                }

                if (dataValue.length > 1 && !dataValue[1].trim().isEmpty()
                        && dataValue[0].trim().equals(LAST_NAME)) {
                    lastName = dataValue[1].trim();
                    newUser.setLastName(lastName);
                }
            }
        }
        return new UserUpdateProfileInformationDto(email, password, firstName, lastName);
    }

    private List<String> parseUpdateRoleData(Message message) {
        final String text = message.getText();
        List<String> updateData = new ArrayList<>();
        Long id = null;
        String roleString = null;

        if (text.contains(USER_ID) && text.contains(USER_ROLE)) {
            String[] data = text.split(Separator.COMMA.getSeparator());

            for (String value : data) {
                String[] dataValue = value.split(Separator.COLON.getSeparator());

                if (dataValue.length > 1 && !dataValue[1].trim().isEmpty()) {
                    if (dataValue[0].equals(USER_ID)) {
                        id = Long.parseLong(dataValue[1].trim());

                        if (id > 0 && userService.existsById(id)) {
                            updateData.add(dataValue[1].trim());
                        } else {
                            throw new TelegramException("The User id parameter is entered "
                                    + "incorrectly, or the user by this ID does not exist.");
                        }
                    }

                    if (dataValue[0].equals(USER_ROLE)) {
                        for (User.Role role : User.Role.values()) {
                            if (dataValue[1].toUpperCase().trim().equals(role.name())) {
                                roleString = role.name();
                                updateData.add(roleString);
                            }
                        }
                    }
                }
            }
        }

        if (roleString == null) {
            throw new TelegramException("The User role parameter is entered incorrectly");
        }
        return updateData;
    }
}

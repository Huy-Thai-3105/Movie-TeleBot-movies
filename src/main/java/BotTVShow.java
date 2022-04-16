import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.vdurmont.emoji.EmojiParser;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BotTVShow {
    private final static String API = "5c54b7e4adc51f25bb50d1136235c1ff";
    private JSONArray TVShowObject ;
    private JSONArray TVShowReview ;
    public int code ;
    public Object previousMessage;
    //constructor
    public BotTVShow(){
        this.TVShowObject = new JSONArray() ;
        this.TVShowReview = new JSONArray() ;
    }

    public JSONArray getTVShowObject(){
        return this.TVShowObject ;
    }

    public JSONArray getTVShowReview() {
        return this.TVShowReview ;
    }

    public SendMessage Message_Start(String chatID, int messageID){
        String message = EmojiParser.parseToUnicode(
                "You want to search some thing about TV Shows? Please enter :\n" +
                        "Type /TVshow + 'your season TV Show you want to find'\n"+
                        "Type /trending_TVshow to see which TV Show is hot in this weekend\n"
        );
        SendMessage messageStart = new SendMessage();
        messageStart.setChatId(chatID);
        messageStart.setText(message);
        return messageStart ;
    }

    public void seachByName(String nameTVShow) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get("https://api.themoviedb.org/" +
                        "3/search/tv" +
                        "?api_key=" + API +
                        "&query=" + nameTVShow)
                         .asJson();
        this.TVShowObject = response.getBody()
                .getObject()
                .getJSONArray("results");
    }

    public void SearchTrendingTVShows() throws UnirestException {
        HttpResponse<JsonNode> response    = Unirest.get(   "https://api.themoviedb.org/" +
                "3/trending/tv/week" +
                "?api_key=" + API).asJson();
        this.TVShowObject = response.getBody().getObject().getJSONArray("results");
    }

    public JSONObject getTvShow(int index) {
        return this.TVShowObject.getJSONObject(index);
    }

    public SendMessage displaySearchList(int index , String chatID){
       if (this.TVShowObject.isEmpty()) {
            SendMessage replyMessage = new SendMessage();
            replyMessage.setChatId(chatID);
            replyMessage.setText(EmojiParser.parseToUnicode("Sorry I can't find this TvShows. Can you check your name or season of this TV show ? "));
            return replyMessage;
        }
        String text = "";
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        for (int i=5*index; i < (5*index + 5); i++) {
            JSONObject Show = this.TVShowObject.getJSONObject(i);
            String nameShow = Show.get("original_name").toString();

            text += i + "/ "+ nameShow + "\n";
            row1.add(new InlineKeyboardButton(String.valueOf(i), null, "TVshowIndex_" + i,
                    null, null, null, null, null));
        }

        if ((index+1)*5 < this.TVShowObject.length() - 1)
            row2.add(new InlineKeyboardButton(">>", null, "TVshow_forward_" + (index + 1),
                    null, null, null, null, null));


        List<List<InlineKeyboardButton>> btnList = new ArrayList<>();
        btnList.add(row1);
        btnList.add(row2);
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup(btnList);

        SendMessage replyMessage = new SendMessage();
        replyMessage.setChatId(chatID);
        replyMessage.setReplyMarkup(allBtn);
        replyMessage.setText(text);

        this.previousMessage = (Object) replyMessage;
        this.code = 0;
        return replyMessage;
    }

    public EditMessageText displaySearchList(int index , String chatID , long messageID){
        String text = "";
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        for (int i=5*index; i < (5*index + 5); i++) {
            JSONObject Show = this.TVShowObject.getJSONObject(i);
            String nameShow = Show.get("original_name").toString();

            text += i + "/ " + nameShow + "\n";
            row1.add(new InlineKeyboardButton(String.valueOf(i), null, "TVshowIndex_" + i,
                    null, null, null, null, null));
        }

        if ((index-1)*5 >= 0)
            row2.add(new InlineKeyboardButton("<<", null, "TVshow_backward_" + (index - 1),
                    null, null, null, null, null));
        if ((index+1)*5 < this.TVShowObject.length() - 1)
            row2.add(new InlineKeyboardButton(">>", null, "TVshow_forward_" + (index + 1),
                    null, null, null, null, null));

        List<List<InlineKeyboardButton>> btnList = new ArrayList<>();
        btnList.add(row1); btnList.add(row2);
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup(btnList);

        EditMessageText replyMessage = new EditMessageText();
        replyMessage.setChatId(chatID);
        replyMessage.setMessageId(Math.toIntExact(messageID));
        replyMessage.setReplyMarkup(allBtn);
        replyMessage.setText(text);

        this.previousMessage = replyMessage;
        this.code = 1;

        return replyMessage;
    }


    // get trailer link video
    private String getTrailerVideo(int index) throws UnirestException {
        JSONObject allTV = this.TVShowObject.getJSONObject(index) ;
        String tvID = allTV.get("id").toString() ;
        //search detail TV show
        HttpResponse<JsonNode> DetailOfShow = Unirest.get("https://api.themoviedb.org/3/tv/" +
                tvID +"/videos" + "?api_key=" + API ).asJson() ;
        JSONObject trailerVideo = new JSONObject();
        JSONArray result = DetailOfShow.getBody()
                .getObject()
                .getJSONArray("results") ;

        if(result.isEmpty()) {
            return "No trailer found" ;
        }
        for (int i=0; i < result.length(); i++) {
            if (result.getJSONObject(i).get("type").equals("Trailer")) {
                trailerVideo = result.getJSONObject(i);
                break;
            }
        }
        if (trailerVideo.has("key"))
            return trailerVideo.get("key").toString();
        return "No trailer found";
    }

    public SendPhoto getDetailShow (int index , String chatID , long messageID) throws ParseException, UnirestException{
        JSONObject Show = this.TVShowObject.getJSONObject(index) ;
        String Name =  Show.get("original_name").toString()  ;
        String overView = Show.get("overview").toString()  ;
        String Vote = Show.get("vote_average").toString();
        String first_air_date= Show.get("first_air_date").toString()  ;
        String logo_path = Show.get("poster_path").toString()  ;
        String TrailerLink = this.getTrailerVideo(index);
        InlineKeyboardButton trailerBtn = new InlineKeyboardButton("Trailer", "https://www.youtube.com/watch?v=" + TrailerLink, null,
                null, null, null, null, null);
        InlineKeyboardButton watchReviewBtn = new InlineKeyboardButton("Watch reviews", null, "/trending_TVshow",
                null, null, null, null, null);
        InlineKeyboardButton addToListBtn = new InlineKeyboardButton("Add to list", null, "add_myList_" + index,
                null, null, null, null, null);
        InlineKeyboardButton Return  = new InlineKeyboardButton("Return", null, "MyList",
                null, null, null, null, null);
        List<InlineKeyboardButton> row1 = new ArrayList<>() ;
        List<InlineKeyboardButton> row2 = new ArrayList<>() ;
        List<List<InlineKeyboardButton>> ListButton = new ArrayList<>() ;

        if (!TrailerLink.equals("No trailer found"))
            row1.add(trailerBtn);

        /*if (this.getUserReview(index))*/
            row1.add(watchReviewBtn);

        if (!first_air_date.isEmpty()) {
            LocalDate first_date = LocalDate.parse(first_air_date);
            if (LocalDate.now().isBefore(first_date))
                row1.add(addToListBtn);
        }
        row2.add(Return);

        ListButton.add(row1);
        ListButton.add(row2);
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup();
        allBtn.setKeyboard(ListButton);

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd");
        Date valueDate = input.parse(first_air_date);
        SimpleDateFormat output = new SimpleDateFormat("dd/MM/yyyy");
        first_air_date = output.format(valueDate);

        InputFile image = new InputFile("https://image.tmdb.org/t/p/w500" + logo_path);
        SendPhoto reply = new SendPhoto(chatID, image);
        reply.setCaption(Name.toUpperCase() + "\n\n" +
                "First air date: " + first_air_date + "\n\n" +
                     //   "episode run time" + episode_run_time + "\n\n" +
           //     "Last air date: " + last_air_date + "\n\n" +
                "Rating: " + Vote  + "\n\n" +
                "Overview: " + overView);

        reply.setReplyMarkup(allBtn);
        return reply;
    }
    public SendMessage BackToList() {

        String text = "";
        String chatId = "";
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup();

        if (this.code == 0) {
            SendMessage message = (SendMessage) this.previousMessage;
            text = message.getText();
            chatId = message.getChatId();
            allBtn = (InlineKeyboardMarkup) message.getReplyMarkup();
        }
        else {
            EditMessageText message = (EditMessageText) this.previousMessage;
            text = message.getText();
            chatId = message.getChatId();
            allBtn = message.getReplyMarkup();
        }
        SendMessage replyMessage = new SendMessage(chatId, text);
        replyMessage.setReplyMarkup(allBtn);
        return replyMessage;
    }

    /////////////////////////////////////////////////
    /////////// --check and get user review -- //////
    public boolean getUserReview(int index) throws UnirestException {
        String Name = this.TVShowObject.getJSONObject(index).get("original_name").toString();
       // String movieYear = this.TVShowObject.getJSONObject(index).get("release_date").toString();
       // movieYear = movieYear.split("-")[0];
        Name = Name.replace(" ", "%20");

        HttpResponse<JsonNode> response = Unirest.get("https://imdb8.p.rapidapi.com/title/find?q="+Name)
                .header("x-rapidapi-host", "imdb8.p.rapidapi.com")
                .header("x-rapidapi-key", "3793fd6a8bmsh3992e80f2f92f34p1ee63ajsn3b0fe00f7804")
                .asJson();
        System.out.println(response.toString());
        JSONObject temp2 =  response.getBody().getObject();
        if (!temp2.has("results"))
            return false;

        JSONArray foundMovies = temp2.getJSONArray("results");
        JSONObject foundMovie = null;
        /*for (int i=0; i < foundMovies.length(); i++) {
            JSONObject t = foundMovies.getJSONObject(i);
            if (t.has("titleType") && t.has("year")) {
                if (t.get("titleType").toString().equals("movie") && t.get("year").toString().equals(movieYear)) {
                    foundMovie = t;
                    break;
                }
            }
        }
        if (foundMovie == null)
            return false;*/

        String movieId = foundMovie.get("id").toString().split("/")[2];

        // get user review form movieId
        response = Unirest.get("https://imdb8.p.rapidapi.com/title/get-user-reviews?tconst=" + movieId)
                .header("x-rapidapi-host", "imdb8.p.rapidapi.com")
                .header("x-rapidapi-key", "3793fd6a8bmsh3992e80f2f92f34p1ee63ajsn3b0fe00f7804")
                .asJson();

        JSONObject temp = response.getBody().getObject();
        if (temp.has("reviews")) {
            this.TVShowReview = temp.getJSONArray("reviews");
            return true;
        }
        return false;
    }


    public SendMessage displayReview(int index, String chatID, long messageId) {
        String userPoint = "unknown", userName = "", userReview = "", reviewTitle = "";

        // check whether it has review or not
        if (this.TVShowReview.length() == 0) {
            SendMessage replyMessage = new SendMessage();
            replyMessage.setChatId(chatID);
            replyMessage.setText(EmojiParser.parseToUnicode("No one watches this movie you stupid head  :clown: :clown: !!\n\nGet you ass out of here"));
            return replyMessage;
        }

        // check the length of each review, if it to long, pass to another review
        JSONObject review;
        int length;
        do {
            review = this.TVShowReview.getJSONObject(index);
            length = review.get("reviewText").toString().length();
            index ++;
        } while (length > 4000);

        // take review value
        if (review.has("author"))
            userName = review.getJSONObject("author")
                    .get("displayName")
                    .toString();
        if (review.has("reviewText"))
            userReview = review.get("reviewText").toString();
        if (review.has("reviewTitle"))
            reviewTitle = review.get("reviewTitle").toString();
        if (review.has("authorRating"))
            userPoint = String.valueOf(review.get("authorRating"));

        // create button to switch to next review
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<List<InlineKeyboardButton>> btnList = new ArrayList<>();
        if (this.TVShowReview.length() > (index + 1)) {
            InlineKeyboardButton forwardBtn = new InlineKeyboardButton(">>", null, "movieReview_forward_" + (index+1),
                    null, null, null, null, null);
            row1.add(forwardBtn);
        }
        row2.add(new InlineKeyboardButton("Return to movie", null, "delete_movieReview",
                null, null, null, null, null));
        btnList.add(row1);
        btnList.add(row2);
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup();
        allBtn.setKeyboard(btnList);

        // return a message
        SendMessage replyMessage = new SendMessage();
        replyMessage.setChatId(chatID);
        replyMessage.setText(EmojiParser.parseToUnicode(":bust_in_silhouette: Account: " + userName + "\n\n"
                + "\t:+1: User score: " + userPoint + "\n\n"
                + "\t:speaking_head_in_silhouette: Review: " + reviewTitle.toUpperCase() + "\n\n" + userReview));
        replyMessage.setReplyMarkup(allBtn);
        return replyMessage;
    }

    public EditMessageText displayReview(int index, String chatID, int messageID, boolean isBackward) {
        String userPoint = "unknown", userName = "", userReview = "", reviewTitle = "";
        JSONObject review;
        int length;

        // check the length of each review, if it to long, pass to another review
        do {
            review = this.TVShowReview.getJSONObject(index);
            length = review.get("reviewText").toString().length();
            if (!isBackward)
                ++index;
            else
                --index;
        } while (length > 4000);

        // take review value
        if (review.has("author"))
            userName = review.getJSONObject("author")
                    .get("displayName")
                    .toString();
        if (review.has("reviewText"))
            userReview = review.get("reviewText").toString();
        if (review.has("reviewTitle"))
            reviewTitle = review.get("reviewTitle").toString();
        if (review.has("authorRating"))
            userPoint = String.valueOf(review.get("authorRating"));


        // create button to switch to next review
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<List<InlineKeyboardButton>> btnList = new ArrayList<>();
        if (index-1 >= 0) {
            InlineKeyboardButton backwardBtn = new InlineKeyboardButton("<<", null, "movieReview_backward_" + (index - 1),
                    null, null, null, null, null);
            row1.add(backwardBtn);
        }
        if (index+1 < this.TVShowReview.length()) {
            InlineKeyboardButton forwardBtn = new InlineKeyboardButton(">>", null, "movieReview_forward_" + (index+1),
                    null, null, null, null, null);
            row1.add(forwardBtn);
        }
        row2.add(new InlineKeyboardButton("Return to movie", null, "delete_movieReview",
                null, null, null, null, null));
        btnList.add(row1);
        btnList.add(row2);
        InlineKeyboardMarkup allBtn = new InlineKeyboardMarkup();
        allBtn.setKeyboard(btnList);

        // return a edit message
        EditMessageText editText = new EditMessageText();
        editText.setChatId(chatID);
        editText.setMessageId(messageID);
        editText.setText(EmojiParser.parseToUnicode(":bust_in_silhouette: Account: " + userName + "\n\n"
                + "\t:+1: User score: " + userPoint + "\n\n"
                + "\t:speaking_head_in_silhouette: Review: " + reviewTitle.toUpperCase() + "\n\n" + userReview));
        editText.setReplyMarkup(allBtn);
        return editText;
    }
}
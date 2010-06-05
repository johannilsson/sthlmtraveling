package com.markupartist.sthlmtraveling.provider.deviation;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;
import android.util.TimeFormatException;

import com.markupartist.sthlmtraveling.utils.HttpManager;
import com.markupartist.sthlmtraveling.utils.StreamUtils;

public class DeviationStore {
    static String TAG = "DeviationStore";
    private static String LINE_PATTERN = "[A-Za-zåäöÅÄÖ ]?([\\d]+)[ A-Z]?";
    private static Pattern sLinePattern = Pattern.compile(LINE_PATTERN);

    public ArrayList<Deviation> getDeviations() 
            throws IOException {
        ArrayList<Deviation> deviations = new ArrayList<Deviation>();

        try {
            String deviationsRawJson = retrieveDeviations();

            JSONArray jsonArray = new JSONArray(deviationsRawJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jsonDeviation = jsonArray.getJSONObject(i);

                    Time created = new Time();
                    created.parse(jsonDeviation.getString("created"));

                    Deviation deviation = new Deviation();
                    deviation.setCreated(created);
                    deviation.setDetails(stripNewLinesAtTheEnd(jsonDeviation.getString("details")).trim());
                    deviation.setHeader(jsonDeviation.getString("header").trim());
                    deviation.setLink(jsonDeviation.getString("link").trim());
                    deviation.setMessageVersion(jsonDeviation.getInt("messageVersion"));
                    deviation.setReference(jsonDeviation.getLong("reference"));
                    deviation.setScope(jsonDeviation.getString("scope").trim());
                    deviation.setScopeElements(jsonDeviation.getString("scopeElements").trim());
                    deviation.setSortOrder(jsonDeviation.getInt("sortOrder"));

                    deviations.add(deviation);
                } catch (TimeFormatException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return deviations;
    }

    private String retrieveDeviations() throws IOException {
        final HttpGet get = new HttpGet(apiEndpoint()
                + "/deviations/?key=" + get(KEY));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting deviations.");
        }

        entity = response.getEntity();
        return StreamUtils.toString(entity.getContent());
    }

    public static ArrayList<Deviation> filterByLineNumbers(
            ArrayList<Deviation> deviations, ArrayList<Integer> lineNumbers) {
        if (lineNumbers.isEmpty()) {
            return deviations;
        }

        ArrayList<Deviation> filteredList = new ArrayList<Deviation>();
        for (Deviation deviation : deviations) {
            ArrayList<Integer> lines = extractLineNumbers(
                    deviation.getScopeElements(), null);
            //Log.d(TAG, "Matching " + lineNumbers.toString() + " against " + lines);
            for (int line : lineNumbers) {
                if (lines.contains(line)) {
                    filteredList.add(deviation);
                }
            }
        }

        return filteredList;
    }

    /**
     * Extract integer from the passed string recursively.
     * @param scope the string
     * @param foundIntegers previous found integer, pass null if you want to 
     * start from scratch
     * @return the found integers or a empty ArrayList if none found
     */
    public static ArrayList<Integer> extractLineNumbers(String scope,
            ArrayList<Integer> foundIntegers) {
        if (foundIntegers == null)
            foundIntegers = new ArrayList<Integer>();

        Matcher matcher = sLinePattern.matcher(scope);
        boolean matchFound = matcher.find(); 

        if (matchFound) {
            foundIntegers.add(Integer.parseInt(matcher.group(1)));
            scope = scope.replaceFirst(matcher.group(1), ""); // remove what we found.
        } else {
            return foundIntegers;
        }

        return extractLineNumbers(scope, foundIntegers);
    }

    private String stripNewLinesAtTheEnd(String value) {
        if (value.endsWith("\n")) {
            value = value.substring(0, value.length() - 2);
            stripNewLinesAtTheEnd(value);
        }
        return value;
    }
    
    static String deviationJson = "[{\"scope\":\"Pendeltåg Södertälje-Märsta-linjen 36\",\"details\":\"Inställda avgångar för pendeltåg 36 2010-02-01 på grund av växelfel:\\nÄlvsjö - Södertälje centrum kl 18:21, 18:36\\nSödertälje centrum - Stockholms central kl 19:02, 19:17\",\"messageVersion\":1,\"created\":\"20100201T065005Z\",\"sortOrder\":1,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038540020\",\"scopeElements\":\"Pendeltåg Södertälje-Märsta-linjen 36\",\"header\":\"Inställda avgångar\",\"reference\":4050000038539973},{\"scope\":\"Buss 709\",\"details\":\"Försening på 10 minuter för buss 709 från Huddinge station kl  19:35 mot Lissma gård på grund av förseningar.\",\"messageVersion\":1,\"created\":\"20100201T064553Z\",\"sortOrder\":7,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038539487\",\"scopeElements\":\"Buss 709\",\"header\":\"Försenad avgång\",\"reference\":4050000038539485},{\"scope\":\"Buss 553\",\"details\":\"Barkarby station - Jakobsbergs station kl 19:35 är inställd 2010-02-01 på grund av olyckshändelse.\\n\",\"messageVersion\":1,\"created\":\"20100201T064523Z\",\"sortOrder\":8,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038539431\",\"scopeElements\":\"Buss 553\",\"header\":\"Inställd avgång\",\"reference\":4050000038539429},{\"scope\":\" Blåbuss 172\",\"details\":\"Försening på 14 minuter för blåbuss 172 från Rågsved kl  19:19 mot Norsborg på grund av vagnfel.\",\"messageVersion\":1,\"created\":\"20100201T063514Z\",\"sortOrder\":9,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038537895\",\"scopeElements\":\" Blåbuss 172\",\"header\":\"Försenad avgång\",\"reference\":4050000038537875},{\"scope\":\"Pendeltåg Gnestalinjen 37, Nynäshamn-Bålsta-linjen35, Södertälje-Märsta-linjen36\",\"details\":\"Förseningar  för pendeltåg 35, 36 och 37 på grund av tidigare växelfel.\\n\",\"messageVersion\":1,\"created\":\"20100201T063437Z\",\"sortOrder\":2,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038537125\",\"scopeElements\":\"Pendeltåg Gnestalinjen 37, Nynäshamn-Bålsta-linjen35, Södertälje-Märsta-linjen36\",\"header\":\"Förseningar\",\"reference\":4050000038537121},{\"scope\":\" Blåbuss 1\",\"details\":\"buss 1 Extra avgång Essingetorget - Frihamnen kl 19:31\",\"messageVersion\":2,\"created\":\"20100201T063136Z\",\"sortOrder\":10,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038539840\",\"scopeElements\":\" Blåbuss 1\",\"header\":\"buss 1\",\"reference\":4050000038536718},{\"scope\":\"Pendeltåg Nynäshamn-Bålsta-linjen 35\",\"details\":\"Inställda avgångar för pendeltåg 35 2010-02-01 på grund av olyckshändelse:\\nStockholms central - Nynäshamn kl 17:49\\nNynäshamn - Stockholms central kl 19:05\",\"messageVersion\":1,\"created\":\"20100201T062610Z\",\"sortOrder\":5,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038536141\",\"scopeElements\":\"Pendeltåg Nynäshamn-Bålsta-linjen 35\",\"header\":\"Inställda avgångar\",\"reference\":4050000038536104},{\"scope\":\" Blåbuss 1\",\"details\":\"buss 1 Extra avgång Essingetorget - Frihamnen kl 19:21\",\"messageVersion\":2,\"created\":\"20100201T062127Z\",\"sortOrder\":11,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038539804\",\"scopeElements\":\" Blåbuss 1\",\"header\":\"buss 1\",\"reference\":4050000038535472},{\"scope\":\"Buss 604, 614, 615, 616\",\"details\":\"Buss 604, 614, 615 och 616 stannar inte vid Tibbleskolan sedan kl 19:06 på grund av framkomlighetsproblem.\\nDetta beräknas pågå till cirka kl 20:06.\",\"messageVersion\":1,\"created\":\"20100201T061108Z\",\"sortOrder\":12,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038533931\",\"scopeElements\":\"Buss 604, 614, 615, 616\",\"header\":\"Indragen hållplats\",\"reference\":4050000038533926},{\"scope\":\"Roslagsbanan Kårstalinjen 27\",\"details\":\"Trafiken går åter enligt tidtabell för Roslagsbanan 27 från kl 19:20 efter tidigare förseningar på grund av vagnfel.\",\"messageVersion\":3,\"created\":\"20100201T060637Z\",\"sortOrder\":13,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038535577\",\"scopeElements\":\"Roslagsbanan Kårstalinjen 27\",\"header\":\"Trafiken går åter enligt tidtabell\",\"reference\":4050000038533343},{\"scope\":\"Pendeltåg Nynäshamn-Bålsta-linjen 35, Södertälje-Märsta-linjen36\",\"details\":\"Trafiken är åter igång för pendeltåg 35 och 36 sedan kl 19:06 efter inställd trafik på grund av olyckshändelse.\",\"messageVersion\":4,\"created\":\"20100201T060524Z\",\"sortOrder\":3,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038535028\",\"scopeElements\":\"Pendeltåg Nynäshamn-Bålsta-linjen 35, Södertälje-Märsta-linjen36\",\"header\":\"Trafiken är åter igång\",\"reference\":4050000038532967},{\"scope\":\" Blåbuss 4\",\"details\":\"Radiohuset - Värtavägen kl 19:00 är inställd 2010-02-01 på grund av framkomlighetsproblem.\\n\",\"messageVersion\":2,\"created\":\"20100201T055506Z\",\"sortOrder\":14,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038531825\",\"scopeElements\":\" Blåbuss 4\",\"header\":\"Inställd avgång\",\"reference\":4050000038531386},{\"scope\":\"Buss 40\",\"details\":\"Buss 40 stannar åter vid Lindvallsplan från kl 18:30.\",\"messageVersion\":2,\"created\":\"20100201T050736Z\",\"sortOrder\":15,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038527131\",\"scopeElements\":\"Buss 40\",\"header\":\"Tidigare indragna hållplatser\",\"reference\":4050000038522854},{\"scope\":\"Buss 40\",\"details\":\"Buss 40 stannar åter vid Lindvallsplan från kl 06:31.\",\"messageVersion\":2,\"created\":\"20100201T050346Z\",\"sortOrder\":16,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038416292\",\"scopeElements\":\"Buss 40\",\"header\":\"Tidigare indragna hållplatser\",\"reference\":4050000038412291},{\"scope\":\" Blåbuss 172\",\"details\":\"Blåbuss 172 mot Skarpnäck stannar inte vid Gubbängens idrottsplats, Getfotsvägen och Gubbängen sedan 2010-01-26 kl 12:03 på grund av väghinder. Hållplats Hökarängen riktning Skarpnäck flyttad till utanför tunnelbanan.\\nDetta beräknas pågå till cirka 2010-02-05 kl 15:00.\",\"messageVersion\":15,\"created\":\"20100201T042357Z\",\"sortOrder\":6,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038409002\",\"scopeElements\":\" Blåbuss 172\",\"header\":\"Indragna hållplatser\",\"reference\":4050000038408997},{\"scope\":\" Närtrafiken 904, 905\",\"details\":\"Närtrafiken 904 och 905 trafikerar inte följande gator från och med 2010-02-01 på grund av framkomlighetsproblem:\\nKarlskronavägen, Ronnebyvägen, Falsterbovägen, Skanörvägen, Strahlenbergsgatan och Boråsvägen.\\n\\n\",\"messageVersion\":1,\"created\":\"20100130T055141Z\",\"sortOrder\":4,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038251338\",\"scopeElements\":\" Närtrafiken 904, 905\",\"header\":\"Indragna hållplatser\",\"reference\":4050000038251335},{\"scope\":\"Buss 123\",\"details\":\"Pga framkomlighetsproblem trafikeras jungfrudansen endast i en riktning. Bussen svänger vänster och går i riktning Vreten oavsett slutdestination.\",\"messageVersion\":1,\"created\":\"20100129T080709Z\",\"sortOrder\":17,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000038086862\",\"scopeElements\":\"Buss 123\",\"header\":\"L123 - Jungfrudansen\",\"reference\":4050000038086860},{\"scope\":\"Roslagsbanan Österskärslinjen 28\",\"details\":\"Trafiken ersätts med buss mellan Österskär och Galoppfältet för Roslagsbanan 28 från och med 2010-01-30 till och med 2010-02-02 på grund av underhållsarbete.\\nErsättningsbussar:\\nBuss 28 stannar vid alla hållplatser på den avstängda sträckan.\\nBuss 28X stannar vid Galoppfältet, Åkersberga, Tunagård och Österskär. \",\"messageVersion\":1,\"created\":\"20100123T082847Z\",\"sortOrder\":18,\"link\":\"http://storningsinformation.sl.se/?DMVID=4050000037343366\",\"scopeElements\":\"Roslagsbanan Österskärslinjen 28\",\"header\":\"Buss mellan Österskär och Galoppfältet\",\"reference\":4050000037343355}]";
}

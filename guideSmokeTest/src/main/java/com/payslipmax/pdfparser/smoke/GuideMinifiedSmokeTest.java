package com.payslipmax.pdfparser.smoke;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * R8 Check 2 (docs/Plan/rule_cards/16_guide_phase_plan.md): green debug tests do not prove the minified app can
 * read the Guide. This drives the installed {@code minifiedTest} app (release R8 config) like a user: open the
 * Guide tab, an area, a case, then its first card, then find that card again through search. A model or serializer
 * R8 removed shows the load error instead and fails here.
 *
 * <p>Black-box on purpose: it finds text on screen and reads the bundle from the app's own assets, so nothing pins
 * an app class against R8. The UI strings come from the app's string files through the generated {@code SmokeStrings}.
 */
@RunWith(AndroidJUnit4.class)
public class GuideMinifiedSmokeTest extends SmokeBase {
    private static final String BUNDLE_ASSET =
            "composeResources/pdfparser.composeapp.generated.resources/files/guide/guide_bundle.json";

    @Test
    public void theMinifiedAppOpensGuideHomeAnAreaACaseAndACard() throws Exception {
        JSONObject bundle = new JSONObject(readBundle());
        JSONObject firstArea = bundle.getJSONArray("nav").getJSONObject(0);
        String areaTitle = SmokeStrings.areaTile(firstArea.getString("id"), firstArea.getString("title")); // Guide Home tiles use short names
        JSONObject firstCase = firstArea.getJSONArray("cases").getJSONObject(0);
        String caseTitle = firstCase.getString("title");
        String cardTitle = cardTitle(bundle, firstCase.getJSONArray("cards").getString(0));

        launchApp();
        dismissIfShown(SmokeStrings.onboardingSkip);
        dismissIfShown(SmokeStrings.onboardingCoachmarkDismiss);

        await(SmokeStrings.tabLabel, "the Guide tab (is the app locked, or blocked by the install guard?)").click();
        assertFalse("the Guide failed to load in the minified build",
                device.wait(Until.hasObject(By.text(SmokeStrings.loadFailedTitle)), SHORT_MS));
        await(SmokeStrings.homeSection, "Guide Home");
        await(areaTitle, "the first area tile").click();
        await(caseTitle, "the first case tile in " + areaTitle).click();
        await(cardTitle, "the first card in the " + caseTitle + " feed").click();
        await(SmokeStrings.sectionKeyPoints, "the card's key points");

        // Search (E4): back to Home with a re-tap of the tab, search for the card's own title, open it from the results.
        await(SmokeStrings.tabLabel, "the Guide tab").click();
        await(SmokeStrings.homeSection, "Guide Home after a tab re-tap");
        device.wait(Until.findObject(By.desc(SmokeStrings.searchOpen)), SHORT_MS).click();
        UiObject2 field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), SHORT_MS);
        assertNotNull("the search field is not shown", field);
        field.setText(cardTitle);
        // The count line ("1 result", "7 results") only exists once results do; no copy of it here, so match its stem.
        assertNotNull("search found nothing for a card's own title",
                device.wait(Until.findObject(By.textContains("result")), LONG_MS));
        // The field holds the title too; the result row is the last node with that text.
        List<UiObject2> shown = device.findObjects(By.text(cardTitle));
        shown.get(shown.size() - 1).click();
        await(SmokeStrings.sectionKeyPoints, "the card opened from the search results");
    }

    /**
     * Plan 22, P9 (the flag-on check the P8 walk did by hand): the pre-claim worksheet works in the minified build. Opens the
     * entry on Guide Home, chooses Temporary duty, picks the first TR rule, sees it chosen and sees the hotel box, so the bundle's preclaim models,
     * the Koin factory and the worksheet screens all survived R8. It fails if {@code LaunchFlags.PRECLAIM_ENABLED} is turned off
     * again (no entry), which is the point: pulling the worksheet is a deliberate edit to this test too.
     */
    @Test
    public void theMinifiedAppOpensThePreClaimWorksheet() throws Exception {
        JSONObject preclaim = new JSONObject(readBundle()).getJSONObject("preclaim");
        String moveTitle = preclaim.getJSONObject("moves").getJSONObject("TD").getString("title");
        String ruleLabel = preclaim.getJSONObject("options").getJSONObject("td_rule").getJSONArray("options")
                .getJSONObject(0).getString("label");

        launchApp();
        dismissIfShown(SmokeStrings.onboardingSkip);
        dismissIfShown(SmokeStrings.onboardingCoachmarkDismiss);

        await(SmokeStrings.tabLabel, "the Guide tab (is the app locked, or blocked by the install guard?)").click();
        await(SmokeStrings.homeSection, "Guide Home");
        await(SmokeStrings.tileTitle, "the pre-claim worksheet tile (is PRECLAIM_ENABLED off, or the Guide locked?)").click();
        await(SmokeStrings.chooseMove, "the move chooser");
        await(moveTitle, "the Temporary duty move").click();
        scrollTo(SmokeStrings.trRuleChoose, "the TR rule picker").click();
        UiObject2 option = device.wait(Until.findObject(By.text(ruleLabel)), SHORT_MS);
        if (option == null) { // the tap can land while the list is still settling; the picker is still closed then
            scrollTo(SmokeStrings.trRuleChoose, "the TR rule picker").click();
            option = await(ruleLabel, "the first TR rule option");
        }
        option.click();
        scrollTo(SmokeStrings.trRule + ": " + ruleLabel, "the chosen rule shown on the picker");
        scrollTo(SmokeStrings.nightlyRate, "the hotel box");
    }

    private static String cardTitle(JSONObject bundle, String cardId) throws Exception {
        JSONArray cards = bundle.getJSONArray("cards");
        for (int i = 0; i < cards.length(); i++) {
            if (cards.getJSONObject(i).getString("id").equals(cardId)) return cards.getJSONObject(i).getString("title");
        }
        throw new AssertionError("the bundle has no card " + cardId);
    }

    private String readBundle() throws Exception {
        Context app = context.createPackageContext(APP_ID, 0);
        try (InputStream in = app.getAssets().open(BUNDLE_ASSET)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

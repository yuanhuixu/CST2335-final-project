package com.example.recipeapp.Recipe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.recipeapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * This class shows the listviews and allows for searching.
 * It extends AppCompatActivity
 */
public class RecipeSearch extends AppCompatActivity {

    private Menu menu;
    private Toolbar toolbar;
    private Button searchButton;
    private EditText searchText;
    private ListView list;
    private ProgressBar progressBar;
    private ArrayList<RecipeEntry> recipes = new ArrayList<RecipeEntry>();
//    private RecipeDatabaseHelper opener;
//    private SimpleCursorAdapter chatAdapter;


    private class FetchRecipeList extends AsyncTask<String, Integer, String> {

        private final String urlTemplate = "https://api.spoonacular.com/recipes/findByIngredients?apiKey=2311513282b7432684777caf629d344a&number=10&ingredients=%s";

        /**
         * This Overrides AsyncTask.doInBackGround()
         * <p>
         * It pulls the search results based on what the droids want you to think.
         * <p>
         * Basically it switches between searching Chicken and Lasagna
         *
         * @param @See AsyncTask.doInBackground()
         * @return @See AsyncTask.doInBackground()
         */
        @Override
        protected String doInBackground(String... params) {
            try {
                String ingredients = params[0];
                String rawUrl = String.format(urlTemplate, ingredients);
                URL url = new URL(rawUrl);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inStream = urlConnection.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                JSONArray json_recipes = new JSONArray(sb.toString());

                for (int j = 0; j < json_recipes.length(); j++) {
                    publishProgress((j+1) * (100 / json_recipes.length()));

                    JSONObject r = json_recipes.getJSONObject(j);
                    RecipeEntry recipe = new RecipeEntry();
                    recipe.id = r.getLong("id");
                    recipe.title = r.getString("title");
                    recipe.imageUrl = r.getString("image");
                    recipes.add(recipe);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            return null;
        }

        /**
         * This method takes the param values to update our progress bar.
         * it keeps the search button and text field out of view while the progress bar is used.
         *
         * @param values
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);

        }

        /**
         * This method Overrides the super class's onPostExecute.
         * It calls the super method and turns back on the visibility for the search button and text
         * It saves the value of the boolean that keeps track of what was last searched sends us back to our listView of the results
         *
         * @param results @See AsyncTask.onPostExecute()
         */
        @Override                   //Type 3 of Inner Created Class
        protected void onPostExecute(String results) {
            super.onPostExecute(results);
            progressBar.setProgress(0);
            ArrayAdapter adapter = new ArrayAdapter(RecipeSearch.this, R.layout.list_item, recipes);
            list.setAdapter(adapter);
            list.deferNotifyDataSetChanged();


            // FIXME
            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
            SharedPreferences.Editor editor = pref.edit();

            editor.commit();

//            Intent intent = new Intent(RecipeAsync.this, RecipeSearch.class);
//            startActivityForResult(intent, 30);

        }

    }

    /**
     * This Overrides the superclass's onCreate method,
     * It sets up the tool bar and button as well as selects the right Table to show in the list view.
     * It sets up the Click Listener for the listview and the search button.
     *
     * @param savedInstanceState @See AppCompatActivity.onCreate()
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        progressBar = findViewById(R.id.recipeSearchProgressBar);

        toolbar = (Toolbar) findViewById(R.id.recipe_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //This is the onClickListener for my List
        list = findViewById(R.id.recipeListView);
        list.setOnItemClickListener((mlist, item, position, id) -> {
            Bundle bundle = new Bundle();
            RecipeEntry entry = recipes.get(position);
            bundle.putLong("id", entry.id);
            bundle.putString("title", entry.title);
            bundle.putString("imageUrl", entry.imageUrl);

            boolean isTablet = findViewById(R.id.recipeFragmentLocation) != null;
            if (isTablet) {
                RecipeDetailFragment fragment = new RecipeDetailFragment();
                fragment.setArguments(bundle);
                fragment.setTablet(true);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.recipeFragmentLocation, fragment)
                        .commit();
            } else { //isPhone
                Intent goToDetail = new Intent(RecipeSearch.this, RecipeEmptyActivity.class);
                goToDetail.putExtras(bundle); //send data to next activity
                startActivity(goToDetail); //make the transition
            }
        });


        searchButton = findViewById(R.id.recipeSearchButton);
        searchText = findViewById(R.id.searchEditText);
        searchButton.setOnClickListener(click ->
        {
            new FetchRecipeList().execute(searchText.getText().toString());
            //show a notification: first parameter is any view on screen. second parameter is the text. Third parameter is the length (SHORT/LONG)
//            Snackbar.make(searchButton, "Searching online for Chicken. That is what you typed right?", Snackbar.LENGTH_LONG).show();
//            Intent nextActivity = new Intent(RecipeSearch.this, RecipeAsync.class);
//            nextActivity.putExtra(RecipeAsync.RECIPE_QUERY, searchText.getText().toString());
//            startActivityForResult(nextActivity, 346); //make the transition

//            list.deferNotifyDataSetChanged();

//            searchButton.setEnabled(false);
//            searchText.setEnabled(false);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayAdapter adapter = new ArrayAdapter(RecipeSearch.this, R.layout.list_item, recipes);
        list.setAdapter(adapter);
        list.deferNotifyDataSetChanged();
    }

    /**
     * This method Overrides the superclass's onCreateOptionsMenu() method
     * It sets up the toolbar and sets the toggleling icon based on the current list showing
     *
     * @param menu @see AppCompatActivity.onCreateOptionsMenu()
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the app bar.
        getMenuInflater().inflate(R.menu.accessible_toolbar, menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This sets actions for what will happen when items in the Toolbar are clicked
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_home:
                // RecipeSearch.setTable = false;
                Intent nextActivity2 = new Intent(RecipeSearch.this, RecipeMain.class);
                startActivityForResult(nextActivity2, 346);
                break;
            case R.id.toolbar_help:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.information))
                        .setMessage(getString(R.string.recipeVersion) + "\n" + getString(R.string.recipeSearchHelp))
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            case R.id.toolbar_fav:
                Intent goToFav = new Intent(RecipeSearch.this, RecipeFavActivity.class);
                startActivity(goToFav);
                break;
//                if (showFave) {
//                    showResults();
//                    menu.getItem(0).setIcon(R.drawable.star_unfilled);
//                } else {
//                    showFavorite();
//                    menu.getItem(0).setIcon(R.drawable.search);
//                }
//                showFave = !showFave;
//                break;
            case R.id.toolbar_about:
                Intent goToAbout = new Intent(RecipeSearch.this, AboutMeActivity.class);
                startActivity(goToAbout);
                break;
            case R.id.toolbar_search:
                Toast.makeText(this, getString(R.string.searchToast), Toast.LENGTH_LONG).show();

        }
        return super.onOptionsItemSelected(item);
    }

}

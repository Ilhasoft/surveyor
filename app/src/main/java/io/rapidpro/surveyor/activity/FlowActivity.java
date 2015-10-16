package io.rapidpro.surveyor.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.text.NumberFormat;

import io.rapidpro.surveyor.R;
import io.rapidpro.surveyor.Surveyor;
import io.rapidpro.surveyor.adapter.FlowListAdapter;
import io.rapidpro.surveyor.data.DBFlow;
import io.rapidpro.surveyor.data.Submission;
import io.rapidpro.surveyor.net.FlowDefinition;
import io.rapidpro.surveyor.ui.BlockingProgress;
import io.rapidpro.surveyor.ui.ViewCache;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class FlowActivity extends BaseActivity {

    private BlockingProgress m_refreshProgress;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        setFlowDetails(getDBFlow());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_flow, menu);
        return true;
    }

    public void setFlowDetails(DBFlow flow){

        String questionString = " Questions";
        if (flow.getQuestionCount() == 1) {
            questionString = " Question";
        }

        ViewCache cache = getViewCache();
        NumberFormat nf = NumberFormat.getInstance();
        cache.setText(R.id.text_flow_name, flow.getName());
        cache.setText(R.id.text_flow_questions, nf.format(flow.getQuestionCount()) + questionString);
        cache.setText(R.id.text_flow_revision, "(v" + nf.format(flow.getRevision()) + ")");

        int submissions = Submission.getPendingSubmissionCount(flow);
        if (submissions > 0) {
            cache.show(R.id.container_pending);
            cache.setButtonText(R.id.button_pending, nf.format(submissions));
        } else {
            cache.hide(R.id.container_pending);
        }

    }

    public void onConfirmFlowRefresh(View view) {

        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_flow_refresh))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        m_refreshProgress = new BlockingProgress(FlowActivity.this,
                                R.string.refresh_title, R.string.refresh_flow, 1);
                        m_refreshProgress.show();

                        // go fetch our DBFlow definition async
                        getRapidProService().getFlowDefinition(flow, new Callback<FlowDefinition>() {
                            @Override
                            public void success(FlowDefinition definition, Response response) {
                                Realm realm = getRealm();
                                realm.beginTransaction();
                                flow.setDefinition(new String(((TypedByteArray) response.getBody()).getBytes()));
                                flow.setRevision(definition.metadata.revision);
                                flow.setName(definition.metadata.name);
                                flow.setQuestionCount(definition.rule_sets.size());
                                realm.copyToRealmOrUpdate(flow);
                                realm.commitTransaction();

                                refresh();

                                m_refreshProgress.incrementProgressBy(1);
                                m_refreshProgress.hide();
                                m_refreshProgress = null;
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Surveyor.LOG.e("Failure fetching: " + error.getMessage() + " BODY: " + error.getBody(), error.getCause());
                            }
                        });
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void onConfirmPendingSubmissions(View view) {

        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.confirm_send_submissions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final File[] submissions = Submission.getPendingSubmissions(flow);

                        final BlockingProgress progress = new BlockingProgress(FlowActivity.this,
                                R.string.submit_title, R.string.submit_body, submissions.length);

                        progress.show();

                        new SubmitSubmissions(FlowActivity.this, submissions, progress).execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void onStartFlow(View view) {
        startActivity(getIntent(this, FlowRunActivity.class));
    }


    public void onConfirmDelete(MenuItem item) {
        final DBFlow flow = getDBFlow();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final Realm realm = getRealm();
        realm.beginTransaction();

        final String uuid = flow.getUuid();

        builder.setMessage(getString(R.string.confirm_flow_delete))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        final BlockingProgress progress = new BlockingProgress(FlowActivity.this,
                                R.string.one_moment, R.string.delete_body, 2);
                        progress.show();

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                flow.removeFromRealm();
                                progress.incrementProgressBy(1);
                                Submission.deleteFlowSubmissions(uuid);
                                progress.incrementProgressBy(1);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                realm.commitTransaction();
                                progress.hide();
                                finish();
                            }
                        }.execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

}
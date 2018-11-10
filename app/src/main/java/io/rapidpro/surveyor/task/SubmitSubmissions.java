package io.rapidpro.surveyor.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;

import io.rapidpro.surveyor.activity.BaseActivity;
import io.rapidpro.surveyor.ui.BlockingProgress;

/**
 * Task for sending submissions to the server
 */
public class SubmitSubmissions extends AsyncTask<String, Void, Void> {

    private BaseActivity m_activity;
    private File[] m_submissions;
    private BlockingProgress m_progress;
    private int m_error;
    private String m_errorMessage;

    public SubmitSubmissions(BaseActivity activity, File[] submissions, BlockingProgress progress) {
        m_activity = activity;
        m_submissions = submissions;
        m_progress = progress;
    }

    @Override
    protected Void doInBackground(String... params) {

        for (File submission : m_submissions) {
            // TODO

            m_progress.incrementProgressBy(1);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        m_progress.dismiss();

        if (m_error > 0) {
            Toast.makeText(m_activity, m_error, Toast.LENGTH_SHORT).show();
        } else if (m_errorMessage != null) {
            Toast.makeText(m_activity, m_errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}

package whereismytransport.whereismycheese;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.Marker;

public class ShowCheesyDialog extends Dialog implements View.OnClickListener {

    private Marker marker;
    public Activity context;
    public Button pickUpCheeseButton, exitButton;
    public TextView tvCheeseNoteText;
    private String note;

    public ShowCheesyDialog(Activity context, Marker marker, String note) {
        super(context);
        this.context = context;
        this.marker = marker;
        this.note = note;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_cheesy_dialog);

        tvCheeseNoteText = (TextView) findViewById(R.id.tvCheeseNoteText);
        pickUpCheeseButton = (Button) findViewById(R.id.pickUpCheeseButton);
        exitButton = (Button) findViewById(R.id.exitDialogButton);
        pickUpCheeseButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
        tvCheeseNoteText.setText(note);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pickUpCheeseButton:
                marker.remove();
                dismiss();
                break;
            case R.id.exitDialogButton:
                dismiss();
                break;
            default:
                break;
        }
    }
}
package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.network.StatusUpdater;
import cgeo.geocaching.network.StatusUpdater.Status;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class StatusFragment extends Fragment {

    @BindView(R.id.status_icon)
    protected ImageView statusIcon;
    @BindView(R.id.status_message)
    protected TextView statusMessage;

    private CompositeDisposable statusSubscription = new CompositeDisposable();
    private Unbinder unbinder;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final ViewGroup statusGroup = (ViewGroup) inflater.inflate(R.layout.status, container, false);
        unbinder = ButterKnife.bind(this, statusGroup);
        statusSubscription.add(AndroidRxUtils.bindFragment(this, StatusUpdater.LATEST_STATUS)
                .subscribe(new Consumer<Status>() {
                    @Override
                    public void accept(final Status status) {
                        // override current status if no live connector is active
                        final Boolean noActiveConnectors = ConnectorFactory.anyLiveConnectorActive();

                        if (status == Status.NO_STATUS && !noActiveConnectors) {
                            statusGroup.setVisibility(View.INVISIBLE);
                            return;
                        }

                        final Resources res = getResources();
                        final String packageName = getActivity().getPackageName();

                        if (status.icon != null || noActiveConnectors) {
                            final int iconId = res.getIdentifier(noActiveConnectors ? "cgeo" : status.icon, "drawable", packageName);
                            if (iconId != 0) {
                                statusIcon.setImageResource(iconId);
                                statusIcon.setVisibility(View.VISIBLE);
                            } else {
                                Log.w("StatusHandler: could not find icon corresponding to @drawable/" + status.icon);
                                statusIcon.setVisibility(View.GONE);
                            }
                        } else {
                            statusIcon.setVisibility(View.GONE);
                        }

                        String message = noActiveConnectors ? "No active connectors!\r\rYou need to activate at least one geocaching service to start using c:geo." : status.message;
                        if (status.messageId != null) {
                            final int messageId = res.getIdentifier(status.messageId, "string", packageName);
                            if (messageId != 0) {
                                message = res.getString(messageId);
                            }
                        }

                        statusMessage.setText(message);
                        statusGroup.setVisibility(View.VISIBLE);

                        if (noActiveConnectors) {
                            statusGroup.setOnClickListener(v -> {
                                // how to start this activity from here?
                                /*
                                final Intent intent = new Intent(fromActivity, SettingsActivity.class);
                                intent.putExtra(INTENT_OPEN_SCREEN, preferenceScreenKey);
                                fromActivity.startActivity(intent);
                                */
                            });
                        } else if (status.url != null) {
                            statusGroup.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(status.url))));
                        } else {
                            statusGroup.setClickable(false);
                        }
                    }
                }));
        return statusGroup;
    }

    @Override
    public void onDestroyView() {
        statusSubscription.clear();
        super.onDestroyView();
        unbinder.unbind();
    }

}

package org.thoughtcrime.securesms.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.AvatarImageView;
import org.thoughtcrime.securesms.components.FromTextView;
import org.thoughtcrime.securesms.groups.GroupId;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.LiveRecipient;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientForeverObserver;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContactSelectionListItem extends LinearLayout implements RecipientForeverObserver {

  @SuppressWarnings("unused")
  private static final String TAG = ContactSelectionListItem.class.getSimpleName();

  private static final Map<RecipientId, ArrayList<ContactSelectionListItem>> duplicateContactsMap = new HashMap<>();

  private AvatarImageView contactPhotoImage;
  private TextView        numberView;
  private FromTextView    nameView;
  private TextView        labelView;
  private CheckBox        checkBox;

  private String        number;
  private int           contactType;
  private LiveRecipient recipient;
  private GlideRequests glideRequests;
  private boolean       multiSelect;

  public ContactSelectionListItem(Context context) {
    super(context);
  }

  public ContactSelectionListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.numberView        = findViewById(R.id.number);
    this.labelView         = findViewById(R.id.label);
    this.nameView          = findViewById(R.id.name);
    this.checkBox          = findViewById(R.id.check_box);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests,
                  @Nullable RecipientId recipientId,
                  int type,
                  String name,
                  String number,
                  String label,
                  int color,
                  boolean multiSelect)
  {
    this.glideRequests = glideRequests;
    this.number        = number;
    this.contactType   = type;
    this.multiSelect   = multiSelect;

    if (type == ContactRepository.NEW_PHONE_TYPE || type == ContactRepository.NEW_USERNAME_TYPE) {
      this.recipient = null;
      this.contactPhotoImage.setAvatar(glideRequests, null, false);
    } else if (recipientId != null) {
      this.recipient = Recipient.live(recipientId);
      this.recipient.observeForever(this);
      name = this.recipient.get().getDisplayName(getContext());
    }

    Recipient recipientSnapshot = recipient != null ? recipient.get() : null;

    this.nameView.setTextColor(color);
    this.numberView.setTextColor(color);
    this.contactPhotoImage.setAvatar(glideRequests, recipientSnapshot, false);

    setText(recipientSnapshot, type, name, number, label);

    if (this.multiSelect){
      this.checkBox.setVisibility(View.VISIBLE);
    } else {
      this.checkBox.setVisibility(View.INVISIBLE);
    }

    addToDuplicatesContactsMap();
  }

  public void setChecked(boolean selected) {
    if(getRecipientId().isPresent()){
      RecipientId recipientId = getRecipientId().get();
      if(duplicateContactsMap.containsKey(recipientId)) {
        ArrayList<ContactSelectionListItem> contactSelectionList = duplicateContactsMap.get(recipientId);
        if (contactSelectionList != null) {
          for (ContactSelectionListItem listItem : contactSelectionList) {
            listItem.checkBox.setChecked(selected);
          }
        }
      }
    } else {
      this.checkBox.setChecked(selected);
    }
  }

  public void setMultiSelect(boolean multiSelect){
    this.multiSelect = multiSelect;
    if (this.multiSelect){
      this.checkBox.setVisibility(View.VISIBLE);
    } else {
      this.checkBox.setVisibility(View.INVISIBLE);
    }
  }

  public void unbind(GlideRequests glideRequests) {
    removeFromDuplicateContactsMaps();

    if (recipient != null) {
      recipient.removeForeverObserver(this);
      recipient = null;
    }
  }

  private void addToDuplicatesContactsMap(){
    if(getRecipientId().isPresent()){
      RecipientId recipientId = getRecipientId().get();
      if (!duplicateContactsMap.containsKey(recipientId)) {
        duplicateContactsMap.put(recipientId, new ArrayList<>());
      }
      duplicateContactsMap.get(recipientId).add(this);
    }
  }

  private void removeFromDuplicateContactsMaps(){
    if(getRecipientId().isPresent()) {
      RecipientId recipientId = getRecipientId().get();
      if (duplicateContactsMap.containsKey(recipientId)) {
        ArrayList<ContactSelectionListItem> contactSelectionList = duplicateContactsMap.get(recipientId);
        if (contactSelectionList != null) {
          contactSelectionList.remove(this);
        }
      }
    }
  }

  @SuppressLint("SetTextI18n")
  private void setText(@Nullable Recipient recipient, int type, String name, String number, String label) {
    if (number == null || number.isEmpty() || GroupId.isEncodedGroup(number)) {
      this.nameView.setEnabled(false);
      this.numberView.setText("");
      this.labelView.setVisibility(View.GONE);
    } else if (type == ContactRepository.PUSH_TYPE) {
      this.numberView.setText(number);
      this.nameView.setEnabled(true);
      this.labelView.setVisibility(View.GONE);
    } else if (type == ContactRepository.NEW_USERNAME_TYPE) {
      this.numberView.setText("@" + number);
      this.nameView.setEnabled(true);
      this.labelView.setText(label);
      this.labelView.setVisibility(View.VISIBLE);
    } else {
      this.numberView.setText(number);
      this.nameView.setEnabled(true);
      this.labelView.setText(label != null && !label.equals("null") ? label : "");
      this.labelView.setVisibility(View.VISIBLE);
    }

    if (recipient != null) {
      this.nameView.setText(recipient);
    } else {
      this.nameView.setText(name);
    }
  }

  public String getNumber() {
    return number;
  }

  public boolean isMultiSelect() {
    return multiSelect;
  }

  public boolean isUsernameType() {
    return contactType == ContactRepository.NEW_USERNAME_TYPE;
  }

  public Optional<RecipientId> getRecipientId() {
    return recipient != null ? Optional.of(recipient.getId()) : Optional.absent();
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient recipient) {
    contactPhotoImage.setAvatar(glideRequests, recipient, false);
    nameView.setText(recipient);
  }
}

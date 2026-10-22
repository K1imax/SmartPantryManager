package com.example.smartpantry;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Locale;

/** Add or edit one pantry item. If an item id is passed in the Intent, it edits that item. */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "item_id";

    private DatabaseHelper dbHelper;
    private EditText etName, etQuantity, etExpiry;
    private Spinner spUnit;
    private PantryItem existing = null; // null means we are adding a new item

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_ingredient);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addEditRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + 40, bars.top + 40, bars.right + 40, bars.bottom + 40);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spUnit = findViewById(R.id.spUnit);
        TextView tvTitle = findViewById(R.id.tvTitle);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        etExpiry.setOnClickListener(v -> showDatePicker());
        etExpiry.setOnLongClickListener(v -> {
            etExpiry.setText("");
            return true;
        });

        // Edit mode: an item id was passed through the Intent
        long itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        if (itemId != -1) {
            existing = dbHelper.getPantryItem(itemId);
        }
        if (existing != null) {
            tvTitle.setText("Edit Ingredient");
            etName.setText(existing.getName());
            double q = existing.getQuantity();
            etQuantity.setText(q == Math.floor(q) ? String.valueOf((long) q) : String.valueOf(q));
            spUnit.setSelection(unitAdapter.getPosition(existing.getUnit()));
            if (existing.getExpiryDate() != null) {
                etExpiry.setText(existing.getExpiryDate());
            }
        }

        btnSave.setOnClickListener(v -> saveItem());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveItem() {
        String name = etName.getText().toString().trim();
        String qtyText = etQuantity.getText().toString().trim();
        String unit = spUnit.getSelectedItem().toString();
        String expiry = etExpiry.getText().toString().trim();

        boolean valid = true;

        // Validate the name
        if (name.isEmpty()) {
            etName.setError("Please enter an ingredient name");
            valid = false;
        } else if (!name.matches("[A-Za-z][A-Za-z \\-]+")) {
            etName.setError("Use letters only (at least 2)");
            valid = false;
        }

        // Validate the quantity
        double quantity = 0;
        if (qtyText.isEmpty()) {
            etQuantity.setError("Please enter a quantity");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(qtyText);
                if (quantity <= 0) {
                    etQuantity.setError("Quantity must be greater than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etQuantity.setError("Enter a valid number");
                valid = false;
            }
        }

        if (!valid) {
            return; // stop here, the errors are shown on the fields
        }

        String expiryValue = expiry.isEmpty() ? null : expiry;

        if (existing == null) {
            dbHelper.addPantryItem(new PantryItem(0, name, quantity, unit, expiryValue));
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            existing.setName(name);
            existing.setQuantity(quantity);
            existing.setUnit(unit);
            existing.setExpiryDate(expiryValue);
            dbHelper.updatePantryItem(existing);
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }
        finish(); // go back to the list, whose onResume refreshes it
    }
}
package com.example.expensemanagement.fragment;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensemanagement.R;
import com.example.expensemanagement.TransactionViewModel;
import com.example.expensemanagement.model.TransactionEntity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionFragment extends Fragment {

    // --- UI Elements ---
    private TabLayout tabLayoutEntry;
    private MaterialButtonToggleGroup toggleType;
    private Button btnSaveTransaction, btnViewHistory, btnPickImage;
    private EditText etAmount, etNote, etRepeat, etCustomCategory;
    private TextInputLayout tilCustomCategory;
    private View btnDate;
    private View btnCatFood, btnCatShopping, btnCatOther;
    private TextView tvDateDisplay;
    private ImageButton btnBack;
    private LinearLayout layoutManualInput, layoutImageInput;
    private ImageView ivPreview;
    private ProgressBar ocrProgressBar;

    // --- Data & Logic ---
    private TransactionViewModel viewModel;
    private String selectedCategoryId = "Ăn uống";
    private Calendar selectedDate = Calendar.getInstance();
    private Bitmap currentBitmapToProcess;
    private Uri cameraImageUri;

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("'Hôm nay, 'dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat dbDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // --- Activity Result Launchers ---
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess && cameraImageUri != null) {
                    handleResultUri(cameraImageUri);
                } else {
                    Toast.makeText(getContext(), "Không chụp được ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleResultUri(uri);
                } else {
                    Toast.makeText(getContext(), "Bạn chưa chọn ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    // --- Lifecycle Methods ---
    public static TransactionFragment newInstance() {
        return new TransactionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
    }

    // --- Initialization ---
    private void initViews(View view) {
        tabLayoutEntry = view.findViewById(R.id.tabLayoutEntry);
        layoutManualInput = view.findViewById(R.id.layoutManualInput);
        layoutImageInput = view.findViewById(R.id.layoutImageInput);
        toggleType = view.findViewById(R.id.toggleType);
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        etRepeat = view.findViewById(R.id.etRepeat);
        etCustomCategory = view.findViewById(R.id.etCustomCategory);
        tilCustomCategory = view.findViewById(R.id.tilCustomCategory);
        btnDate = view.findViewById(R.id.btnDate);
        tvDateDisplay = view.findViewById(R.id.tvDateDisplay);
        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        btnBack = view.findViewById(R.id.btnBack);
        btnCatFood = view.findViewById(R.id.btnCatFood);
        btnCatShopping = view.findViewById(R.id.btnCatShopping);
        btnCatOther = view.findViewById(R.id.btnCatOther);
        btnPickImage = view.findViewById(R.id.btnPickImage);
        ivPreview = view.findViewById(R.id.ivPreview);
        ocrProgressBar = view.findViewById(R.id.ocrProgressBar);

        if (tvDateDisplay != null) {
            tvDateDisplay.setText(displayDateFormat.format(selectedDate.getTime()));
        }

        if (toggleType != null) {
            toggleType.check(R.id.btnTypeExpense);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        }

        tabLayoutEntry.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isManual = (tab.getPosition() == 0);
                layoutManualInput.setVisibility(isManual ? View.VISIBLE : View.GONE);
                layoutImageInput.setVisibility(isManual ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnPickImage.setOnClickListener(v -> {
            if (currentBitmapToProcess == null) {
                showSourcePicker();
            } else {
                processImageOCR(currentBitmapToProcess);
            }
        });

        ivPreview.setOnClickListener(v -> {
            if (currentBitmapToProcess != null) {
                showSourcePicker();
            }
        });

        btnDate.setOnClickListener(v -> showDatePicker());
        btnCatFood.setOnClickListener(v -> selectCategory("Ăn uống", false));
        btnCatShopping.setOnClickListener(v -> selectCategory("Mua sắm", false));
        btnCatOther.setOnClickListener(v -> selectCategory("Khác", true));

        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                btnSaveTransaction.setText(
                        checkedId == R.id.btnTypeExpense ? "Thêm giao dịch chi" : "Thêm giao dịch thu"
                );
                updateButtonState();
            }
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateButtonState();
            }
        });

        btnSaveTransaction.setOnClickListener(v -> saveTransaction());

        btnViewHistory.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, TransactionHistoryFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // --- Transaction Business Logic ---
    private void selectCategory(String categoryName, boolean isOther) {
        selectedCategoryId = categoryName;
        if (tilCustomCategory != null) {
            tilCustomCategory.setVisibility(isOther ? View.VISIBLE : View.GONE);
        }
    }

    private void updateButtonState() {
        if (etAmount == null || btnSaveTransaction == null) return;

        String amountStr = etAmount.getText().toString().trim();
        boolean hasAmount = !amountStr.isEmpty() && !amountStr.equals("0");

        btnSaveTransaction.setEnabled(hasAmount);
        btnSaveTransaction.setBackgroundTintList(
                requireContext().getColorStateList(hasAmount ? R.color.accent_pink : R.color.divider)
        );
        btnSaveTransaction.setTextColor(
                requireContext().getColor(hasAmount ? R.color.white : R.color.text_hint)
        );
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateDisplay();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        if (tvDateDisplay != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault());
            tvDateDisplay.setText(sdf.format(selectedDate.getTime()));
        }
    }

    private void saveTransaction() {
        if (etAmount == null) return;

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);
        String note = etNote != null ? etNote.getText().toString().trim() : "";
        String repeat = etRepeat != null ? etRepeat.getText().toString().trim() : "";
        String type = (toggleType != null && toggleType.getCheckedButtonId() == R.id.btnTypeIncome)
                ? "income" : "expense";
        String date = dbDateFormat.format(selectedDate.getTime());

        String category = selectedCategoryId;
        if (selectedCategoryId.equals("Khác") && etCustomCategory != null) {
            category = etCustomCategory.getText().toString().trim();
            if (category.isEmpty()) category = "Khác";
        }

        String finalNote = note;
        if (!repeat.isEmpty()) {
            finalNote += " [Lặp: " + repeat + "]";
        }

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        TransactionEntity entity = new TransactionEntity(
                UUID.randomUUID().toString(),
                userId,
                category,
                amount,
                type,
                finalNote,
                date,
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()),
                ""
        );

        viewModel.insert(entity);
        Toast.makeText(getContext(), "Đã lưu giao dịch thành công!", Toast.LENGTH_SHORT).show();
        clearInputs();
    }

    private void clearInputs() {
        if (etAmount != null) etAmount.setText("");
        if (etNote != null) etNote.setText("");
        if (etRepeat != null) etRepeat.setText("");
        if (etCustomCategory != null) etCustomCategory.setText("");
        if (tilCustomCategory != null) tilCustomCategory.setVisibility(View.GONE);
    }

    // --- Image Processing & OCR Logic ---
    private void showSourcePicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_image_source_picker, null);

        view.findViewById(R.id.btnSourceCamera).setOnClickListener(v -> {
            openCamera();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnSourceGallery).setOnClickListener(v -> {
            openGallery();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void openCamera() {
        try {
            String fileName = "receipt_" + System.currentTimeMillis() + ".jpg";
            File photoFile = new File(requireContext().getCacheDir(), fileName);

            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    photoFile
            );

            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Không mở được camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void handleResultUri(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source =
                        ImageDecoder.createSource(requireContext().getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().getContentResolver(),
                        uri
                );
            }

            currentBitmapToProcess = applyGrayscaleFilter(bitmap);
            ivPreview.setImageBitmap(currentBitmapToProcess);

            btnPickImage.setText("BẮT ĐẦU NHẬN DIỆN");
            btnPickImage.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.accent_pink)
                    )
            );
            btnPickImage.setTextColor(Color.WHITE);

            Toast.makeText(getContext(),
                    "Đã tải ảnh thành công! Nhấn nút để bắt đầu quét.",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap applyGrayscaleFilter(Bitmap src) {
        Bitmap.Config config = src.getConfig();
        if (config == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE)) {
            config = Bitmap.Config.ARGB_8888;
        }
        
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), config);
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return dest;
    }

    private void processImageOCR(Bitmap bitmap) {
        if (ocrProgressBar != null) ocrProgressBar.setVisibility(View.VISIBLE);
        btnPickImage.setEnabled(false);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    if (ocrProgressBar != null) ocrProgressBar.setVisibility(View.GONE);

                    String resultText = visionText.getText();
                    if (resultText == null || resultText.trim().isEmpty()) {
                        Toast.makeText(getContext(),
                                "Không tìm thấy chữ. Hãy thử chọn ảnh khác.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        smartParseTransaction(resultText);
                        resetImagePicker();
                    }
                })
                .addOnFailureListener(e -> {
                    if (ocrProgressBar != null) ocrProgressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Lỗi nhận diện: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    recognizer.close();
                    btnPickImage.setEnabled(true);
                });
    }

    private void resetImagePicker() {
        currentBitmapToProcess = null;
        if (ivPreview != null) {
            ivPreview.setImageDrawable(null);
        }
        btnPickImage.setText("Chọn ảnh hóa đơn");
        btnPickImage.setBackgroundTintList(
                ContextCompat.getColorStateList(requireContext(), R.color.divider)
        );
        btnPickImage.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary)
        );
    }

    private void smartParseTransaction(String rawText) {
        // --- Parse Amount ---
        Pattern amountKeywordPattern = Pattern.compile(
                "(?i)(?:tổng cộng|thanh toán|số tiền|amount|total|sum)[:\\s]*([\\d.,]+)"
        );
        Matcher keywordMatcher = amountKeywordPattern.matcher(rawText);
        String foundAmount = "";

        if (keywordMatcher.find()) {
            foundAmount = keywordMatcher.group(1).replaceAll("[.,]", "");
        } else {
            Pattern anyAmountPattern = Pattern.compile("(\\d{1,3}([\\.,]\\d{3})+)");
            Matcher anyMatcher = anyAmountPattern.matcher(rawText);
            long maxVal = 0;

            while (anyMatcher.find()) {
                try {
                    long val = Long.parseLong(anyMatcher.group(1).replaceAll("[\\.,]", ""));
                    if (val > maxVal) {
                        maxVal = val;
                        foundAmount = String.valueOf(val);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!foundAmount.isEmpty()) {
            etAmount.setText(foundAmount);
        }

        // --- Parse Date ---
        // Patterns: dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
        Pattern datePattern = Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})");
        Matcher dateMatcher = datePattern.matcher(rawText);
        boolean dateFound = false;
        
        if (dateMatcher.find()) {
            String d = dateMatcher.group(1);
            String m = dateMatcher.group(2);
            String y = dateMatcher.group(3);
            
            try {
                int day = Integer.parseInt(d);
                int month = Integer.parseInt(m) - 1; // Calendar month is 0-indexed
                int year = Integer.parseInt(y);
                
                selectedDate.set(year, month, day);
                updateDateDisplay();
                dateFound = true;
            } catch (Exception ignored) {}
        }

        // Feedback to user
        if (!foundAmount.isEmpty() || dateFound) {
            StringBuilder msg = new StringBuilder("Đã trích xuất: ");
            if (!foundAmount.isEmpty()) msg.append("Số tiền ");
            if (!foundAmount.isEmpty() && dateFound) msg.append("và ");
            if (dateFound) msg.append("Ngày");
            
            Toast.makeText(getContext(), msg.toString(), Toast.LENGTH_LONG).show();
            tabLayoutEntry.getTabAt(0).select();
        } else {
            Toast.makeText(getContext(),
                    "Không tìm thấy thông tin phù hợp trên hóa đơn",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
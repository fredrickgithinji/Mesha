package com.dzovah.mesha.Activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dzovah.mesha.Database.Entities.Transaction;
import com.dzovah.mesha.Database.MeshaDatabase;
import com.dzovah.mesha.R;
import com.dzovah.mesha.Activities.Adapters.TransactionAdapter;
import java.util.List;

public class AnalysisActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        recyclerView = findViewById(R.id.rvTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with context
        transactionAdapter = new TransactionAdapter(this);
        recyclerView.setAdapter(transactionAdapter);

        // Fetch transactions from database
        new Thread(() -> {
            transactionsList = MeshaDatabase.Get_database(this)
                    .transactionDao()
                    .getAllTransactionsByEntryTime();

            // Update UI on the main thread
            runOnUiThread(() -> transactionAdapter.setTransactions(transactionsList));
        }).start();
    }
}
 /* The crash is caused by a **NullPointerException** in your `TransactionAdapter` at **line 80**. The error indicates that `betaAccountIcon` is `null`, and you're trying to call:

        ```java
betaAccountIcon.replace("Assets/", "");
```
on a `null` value.

---

        ## **🔥 How to Fix It**
        ### **Option 1: Ensure `betaAccountIcon` is Initialized**
Modify `onBindViewHolder()` in `TransactionAdapter.java`:

        ```java
if (betaAccountIcon != null) {
        try {
String iconPath = betaAccountIcon.replace("Assets/", ""); // ✅ Prevent crash
InputStream is = context.getAssets().open(iconPath);
        holder.transaction_icon.setImageBitmap(BitmapFactory.decodeStream(is));
        is.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
            } else {
            holder.transaction_icon.setImageResource(R.drawable.default_icon); // ✅ Provide a fallback icon
}
        ```
        🔹 **What This Fix Does:**
        - If `betaAccountIcon` is `null`, it skips the `replace()` function to prevent a crash.
- Instead, it sets a **default icon** (`R.drawable.default_icon` → Replace with your actual fallback icon).

        ---

        ### **Option 2: Ensure `setBetaAccountIcon(String icon)` is Called**
In `AnalysisActivity.java`, **before setting the adapter**, make sure you're setting a proper icon:

        ```java
transactionAdapter.setBetaAccountIcon("Assets/default_icon.png"); // Replace with actual path
```

        ---

        ### **🚀 Summary**
        - The crash happens because `betaAccountIcon` is `null`.
        - **Fix 1:** Check if `betaAccountIcon` is `null` before calling `replace()`.
        - **Fix 2:** Ensure `setBetaAccountIcon()` is called before binding transactions.

Try these fixes and let me know if the issue persists! 🚀

  */

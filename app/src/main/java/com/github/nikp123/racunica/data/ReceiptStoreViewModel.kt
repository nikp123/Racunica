import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.github.nikp123.racunica.data.Receipt
import com.github.nikp123.racunica.data.ReceiptDAO
import com.github.nikp123.racunica.data.AppDatabase
import com.github.nikp123.racunica.data.ReceiptStore
import kotlinx.coroutines.launch

class ReceiptStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val receiptDAO: ReceiptDAO = AppDatabase.getDatabase(application).receiptDAO()
    private val receiptStoresLive: LiveData<List<ReceiptStore>> =
        receiptDAO.readAllDataWithStores() // Assuming this returns LiveData
    val receiptStores: LiveData<List<ReceiptStore>> = receiptStoresLive

    fun addReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptDAO.insert(receipt)
        }
    }
}
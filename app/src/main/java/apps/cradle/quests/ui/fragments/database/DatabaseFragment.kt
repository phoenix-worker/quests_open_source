package apps.cradle.quests.ui.fragments.database

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.database.QuestsDatabase
import apps.cradle.quests.databinding.FragmentDatabaseBinding
import apps.cradle.quests.ui.dialogs.ConfirmationDialog
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.utils.events.EventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseFragment : Fragment() {

    private lateinit var binding: FragmentDatabaseBinding
    private lateinit var exportPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var importPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var exportDbLauncher: ActivityResultLauncher<Intent>
    private lateinit var importDbLauncher: ActivityResultLauncher<Intent>
    private val databaseVM by viewModels<DatabaseViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDatabaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerLaunchers()
        setListeners()
        setObservers()
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        val isNewApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        binding.buttonExport.setOnClickListener {
            if (isNewApi) pickPathForExport()
            else checkPermissions(exportPermissionLauncher) { pickPathForExport() }
        }
        binding.buttonImport.setOnClickListener {
            if (isNewApi) pickFileForImport()
            else checkPermissions(importPermissionLauncher) { pickFileForImport() }
        }
    }

    private fun setObservers() {
        databaseVM.exportStateEvent.observe(viewLifecycleOwner,
            EventObserver { onFileOperationStateEvent(it, R.string.toastDbExportSuccess) })
        databaseVM.importStateEvent.observe(viewLifecycleOwner,
            EventObserver { onFileOperationStateEvent(it, R.string.toastDbImportSuccess) })
    }

    private fun onFileOperationStateEvent(
        state: DatabaseViewModel.State,
        successStringRes: Int
    ) {
        when (state) {
            DatabaseViewModel.State.SUCCESS ->
                Toast.makeText(requireActivity(), successStringRes, Toast.LENGTH_SHORT).show()

            DatabaseViewModel.State.ERROR -> {
                Toast.makeText(
                    requireActivity(),
                    R.string.toastDbFileOperationError,
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    private fun checkPermissions(
        launcher: ActivityResultLauncher<String>,
        onPermissionGranted: () -> Unit
    ) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> onPermissionGranted.invoke()

            else -> launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun registerLaunchers() {
        exportPermissionLauncher =
            registerForActivityResult(RequestPermission()) { permissionGranted ->
                if (!permissionGranted) showGoToSettingsDialog()
                else pickPathForExport()
            }
        importPermissionLauncher =
            registerForActivityResult(RequestPermission()) { permissionGranted ->
                if (!permissionGranted) showGoToSettingsDialog()
                else pickFileForImport()
            }
        exportDbLauncher = registerForActivityResult(StartActivityForResult()) {
            onPathForExportPicked(it.data)
        }
        importDbLauncher = registerForActivityResult(StartActivityForResult()) {
            onFileForImportPicked(it.data)
        }
    }

    private fun showGoToSettingsDialog() {
        ConfirmationDialog(
            title = getString(R.string.permissionsDialogTitle),
            message = getString(R.string.permissionsDialogMessage),
            positiveButtonText = getString(R.string.buttonGoTo),
            negativeButtonText = getString(R.string.buttonCancel),
            onPositiveButtonClicked = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        ).show(childFragmentManager, "permissions_info_dialog")
    }

    private fun pickPathForExport() {
        val fileName = "QUESTS_DATABASE (${getDateString()}).db"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        try {
            exportDbLauncher.launch(intent)
        } catch (exc: java.lang.Exception) {
            showNoFileManagerToast()
        }
    }

    private fun pickFileForImport() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            importDbLauncher.launch(intent)
        } catch (exc: java.lang.Exception) {
            showNoFileManagerToast()
        }
    }

    private fun onPathForExportPicked(intent: Intent?) {
        intent?.run {
            data?.run {
                val currentDb = requireActivity().getDatabasePath(QuestsDatabase.dbName)
                databaseVM.exportDatabase(currentDb, this)
            }
        }
    }

    private fun onFileForImportPicked(intent: Intent?) {
        intent?.data?.run {
            if (isFileIsFine(this)) {
                SlideToPerformDialog(
                    titleText = getString(R.string.databaseImportDialogTitle),
                    messageText = getString(R.string.databaseImportDialogMessage),
                    action = getString(R.string.databaseImportDialogAction),
                    onSliderFinishedListener = {
                        val currentDb = requireActivity().getDatabasePath(QuestsDatabase.dbName)
                        databaseVM.importDatabase(currentDb, this)
                    }
                ).show(childFragmentManager, "slide_to_perform_dialog")
            }
        }
    }

    private fun isFileIsFine(uri: Uri): Boolean {
        return try {
            val cr = requireActivity().contentResolver
            val cursor = cr.query(uri, null, null, null, null)
                ?: throw IllegalArgumentException("Не удалось получить информацию о файле.")
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val fileName = cursor.getString(nameIndex)
            val fileSize = cursor.getLong(sizeIndex)
            cursor.close()
            hasPermittedType(fileName) && hasPermittedSize(fileSize)
        } catch (exc: Exception) {
            Toast.makeText(requireActivity(), exc.message, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun hasPermittedSize(fileSizeBytes: Long): Boolean {
        if (fileSizeBytes <= MAX_FILE_SIZE_BYTES) return true
        else throw IllegalArgumentException(
            getString(
                R.string.errorTooBigFile,
                (MAX_FILE_SIZE_BYTES / (1024L * 1024)).toString()
            )
        )
    }

    private fun hasPermittedType(fileName: String): Boolean {
        val parts = fileName.split(".")
        val extension = parts.last().lowercase()
        if (extension == DATABASE_EXTENSION) return true
        else throw IllegalArgumentException(getString(R.string.toastWrongFileFormatForImport))
    }

    private fun getDateString(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showNoFileManagerToast() {
        Toast.makeText(
            requireActivity(),
            R.string.toastNoFileManager,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {

        const val DATABASE_EXTENSION = "db"
        const val MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L

    }

}
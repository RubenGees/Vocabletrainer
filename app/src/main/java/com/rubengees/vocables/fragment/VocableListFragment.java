package com.rubengees.vocables.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.rubengees.vocables.R;
import com.rubengees.vocables.activity.TransferActivity;
import com.rubengees.vocables.adapter.UnitAdapter;
import com.rubengees.vocables.adapter.VocableAdapter;
import com.rubengees.vocables.adapter.VocableListAdapter;
import com.rubengees.vocables.core.Core;
import com.rubengees.vocables.data.UndoManager;
import com.rubengees.vocables.data.VocableManager;
import com.rubengees.vocables.dialog.DeleteDialog;
import com.rubengees.vocables.dialog.ExportDialog;
import com.rubengees.vocables.dialog.ImportDialog;
import com.rubengees.vocables.dialog.SortDialog;
import com.rubengees.vocables.dialog.UnitDialog;
import com.rubengees.vocables.dialog.VocableDialog;
import com.rubengees.vocables.enumeration.SortMode;
import com.rubengees.vocables.pojo.Unit;
import com.rubengees.vocables.pojo.Vocable;
import com.rubengees.vocables.utils.AnimationUtils;
import com.rubengees.vocables.utils.ImportTask;
import com.rubengees.vocables.utils.SwipeToDismissTouchListener;
import com.rubengees.vocables.utils.Utils;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VocableListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VocableListFragment extends MainFragment implements UnitAdapter.OnItemClickListener, VocableAdapter.OnItemClickListener,
        VocableDialog.VocableDialogCallback, UnitDialog.UnitDialogCallback, SortDialog.SortDialogCallback, DeleteDialog.DeleteDialogCallback, ImportTask.OnImportFinishedListener {

    private static final String VOCABLE_DIALOG = "vocable_dialog";
    private static final String UNIT_DIALOG = "unit_dialog";
    private static final String DELETE_DIALOG = "delete_dialog";
    private static final String SORT_DIALOG = "sort_dialog";
    private static final String EXPORT_DIALOG = "export_dialog";
    private static final String IMPORT_DIALOG = "import_dialog";
    private static final String SORT_MODE = "sort_mode";
    private static final String CURRENT_UNIT = "current_unit";
    private RecyclerView recycler;
    private FloatingActionButton fab;
    private View header;
    private TextView unitTitle;

    private VocableListAdapter adapter;
    private VocableManager vocableManager;

    private SortMode mode;

    private ViewGroup root;

    public VocableListFragment() {
        // Required empty public constructor
    }

    public static VocableListFragment newInstance() {
        return new VocableListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mode = (SortMode) savedInstanceState.getSerializable(SORT_MODE);

            VocableDialog vocableDialog = (VocableDialog) getFragmentManager().findFragmentByTag(
                    VOCABLE_DIALOG);
            UnitDialog unitDialog = (UnitDialog) getFragmentManager().findFragmentByTag(UNIT_DIALOG);
            SortDialog sortDialog = (SortDialog) getFragmentManager().findFragmentByTag(SORT_DIALOG);
            DeleteDialog deleteDialog = (DeleteDialog) getFragmentManager().findFragmentByTag(DELETE_DIALOG);

            if (vocableDialog != null) {
                vocableDialog.setCallback(this);
            }

            if (unitDialog != null) {
                unitDialog.setCallback(this);
            }

            if (sortDialog != null) {
                sortDialog.setCallback(this);
            }

            if (deleteDialog != null) {
                deleteDialog.setCallback(this);
            }
        } else {
            mode = SortMode.TITLE;
        }

        vocableManager = Core.getInstance(getActivity()).getVocableManager();

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_vocable_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_vocable_list_delete:
                DeleteDialog deleteDialog = DeleteDialog.newInstance();

                deleteDialog.setCallback(this);
                deleteDialog.show(getFragmentManager(), DELETE_DIALOG);
                return true;
            case R.id.action_vocable_list_export:
                Intent exportIntent = new Intent(getActivity(), TransferActivity.class);
                exportIntent.putExtra("isImport", false);
                startActivityForResult(exportIntent, TransferActivity.REQUEST_EXPORT);
                return true;
            case R.id.action_vocable_list_import:
                Intent importIntent = new Intent(getActivity(), TransferActivity.class);
                importIntent.putExtra("isImport", true);
                startActivityForResult(importIntent, TransferActivity.REQUEST_IMPORT);
                return true;
            case R.id.action_vocable_list_sort:
                SortDialog sortDialog = SortDialog.newInstance(mode);

                sortDialog.setCallback(this);
                sortDialog.show(getFragmentManager(), SORT_DIALOG);
                return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(SORT_MODE, mode);
        if (adapter instanceof VocableAdapter) {
            outState.putParcelable(CURRENT_UNIT, ((VocableAdapter) adapter).getUnit());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        root = (ViewGroup) inflater.inflate(R.layout.fragment_vocable_list, container, false);

        recycler = (RecyclerView) root.findViewById(R.id.fragment_vocable_list_recycler);
        fab = (FloatingActionButton) root.findViewById(R.id.fab);

        header = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_vocable_list_header, container, false);
        ImageButton back = (ImageButton) header.findViewById(R.id.fragment_vocable_list_header_back);
        unitTitle = (TextView) header.findViewById(R.id.fragment_vocable_list_header_title);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUnitAdapter();
            }
        });

        setupRecycler(savedInstanceState);
        setupFAB();

        if (getUndoManager().size() > 0) {
            showSnackbar();
        }

        return root;
    }

    private void setupRecycler(Bundle savedInstanceState) {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), Utils.getSpanCount(getActivity()));

        recycler.setLayoutManager(layoutManager);
        recycler.setHasFixedSize(true);

        SwipeToDismissTouchListener listener = new SwipeToDismissTouchListener(recycler, new SwipeToDismissTouchListener.DismissCallbacks() {
            @Override
            public SwipeToDismissTouchListener.SwipeDirection canDismiss(int position) {
                return SwipeToDismissTouchListener.SwipeDirection.BOTH;
            }

            @Override
            public void onDismiss(RecyclerView view, List<SwipeToDismissTouchListener.PendingDismissData> dismissData) {
                for (SwipeToDismissTouchListener.PendingDismissData pendingDismissData : dismissData) {
                    if (adapter instanceof VocableAdapter) {
                        Unit unit = ((VocableAdapter) adapter).getUnit();
                        Vocable vocable = ((VocableAdapter) adapter).get(pendingDismissData.position);

                        getUndoManager().add(unit, vocable);
                        unit.remove(vocable);
                        vocableManager.vocableRemoved(unit, vocable);
                    } else {
                        Unit unit = ((UnitAdapter) adapter).get(pendingDismissData.position);

                        getUndoManager().add(unit);
                        vocableManager.removeUnit(unit);
                    }

                    adapter.remove(pendingDismissData.position);

                    showSnackbar();
                }

                checkAdapter();
            }
        });

        recycler.addOnItemTouchListener(listener);

        if (savedInstanceState != null) {
            Unit current = savedInstanceState.getParcelable(CURRENT_UNIT);

            if (current == null) {
                setUnitAdapter();
            } else {
                setVocableAdapter(current);
            }
        } else {
            setUnitAdapter();
        }

        checkAdapter();
    }

    private void showSnackbar() {
        int amount = getUndoManager().size();

        SnackbarManager.show(Snackbar.with(getActivity()).actionColor(getResources().getColor(R.color.accent)).actionLabel("Undo")
                .text(amount + " " + (amount == 1 ? "Vocable" : "Vocables") + " " + "deleted").actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        vocableManager.addUnits(getUndoManager().getUnits());
                        getUndoManager().clear();

                        onImportFinished(null);
                    }
                }).eventListener(new EventListener() {
                    private boolean replacing = false;

                    @Override
                    public void onShow(Snackbar snackbar) {
                        AnimationUtils.translateY(fab, -snackbar.getHeight(), 300, new DecelerateInterpolator(1.5f));
                    }

                    @Override
                    public void onShowByReplace(Snackbar snackbar) {

                    }

                    @Override
                    public void onShown(Snackbar snackbar) {

                    }

                    @Override
                    public void onDismiss(Snackbar snackbar) {
                        AnimationUtils.translateY(fab, snackbar.getHeight(), 300, new AccelerateInterpolator(1.5f));
                    }

                    @Override
                    public void onDismissByReplace(Snackbar snackbar) {
                        replacing = true;
                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {
                        if (!replacing) {
                            getUndoManager().clear();
                        }
                        replacing = false;
                    }
                }), root);
    }

    private void setupFAB() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Integer unitId = null;

                if (adapter instanceof VocableAdapter) {
                    unitId = ((VocableAdapter) adapter).getUnit().getId();
                }

                showVocableDialog(unitId, null, null);
            }
        });
        fab.setImageResource(R.drawable.ic_add);
        AnimationUtils.animate(fab, Techniques.Landing, 500, 0, null);
    }

    private void setUnitAdapter() {
        getToolbarActivity().collapseToolbar();

        adapter = new UnitAdapter(vocableManager.getUnitList(), mode, this);

        recycler.setAdapter(adapter);
    }

    private void setVocableAdapter(Unit unit) {
        getToolbarActivity().expandToolbar();
        getToolbarActivity().setToolbarView(header);

        adapter = new VocableAdapter(unit, mode, this);

        recycler.setAdapter(adapter);
        unitTitle.setText(unit.getTitle());
    }

    private void checkAdapter() {
        if (adapter.isEmpty()) {
            if (adapter instanceof VocableAdapter) {
                setUnitAdapter();
            } else if (adapter instanceof UnitAdapter) {
                //TODO Show Empty-View
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (adapter instanceof VocableAdapter) {
            setUnitAdapter();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onItemClick(Unit unit) {
        setVocableAdapter(unit);
    }

    @Override
    public void onItemEdit(Unit unit, int pos) {
        UnitDialog dialog = UnitDialog.newInstance(unit.getId(), pos);

        dialog.setCallback(this);
        dialog.show(getFragmentManager(), UNIT_DIALOG);
    }

    @Override
    public void onInfoClick(Unit unit) {

    }

    @Override
    public void onItemClick(Unit unit, Vocable vocable, int pos) {
        showVocableDialog(unit.getId(), vocable, pos);
    }

    @Override
    public void onInfoClick(Vocable vocable) {

    }

    @Override
    public void onVocableAdded(Unit unit, Vocable vocable) {
        unit.add(vocable);

        boolean unitFound;
        if (vocableManager.getUnit(unit.getId()) == null) {
            unitFound = vocableManager.addUnit(unit);
        } else {
            vocableManager.vocableAdded(unit, vocable);
            unitFound = true;
        }

        if (adapter instanceof VocableAdapter) {
            ((VocableAdapter) adapter).add(vocable);
        } else if (adapter instanceof UnitAdapter) {
            if (!unitFound) {
                ((UnitAdapter) adapter).add(unit);
            }
        }
    }

    @Override
    public void onVocableChanged(Unit newUnit, Unit oldUnit, Vocable vocable, int pos) {
        vocableManager.updateVocable(oldUnit, newUnit, vocable);

        if (oldUnit == newUnit) {
            if (adapter instanceof VocableAdapter) {
                ((VocableAdapter) adapter).update(vocable, pos);
            }
        } else {
            if (adapter instanceof VocableAdapter) {
                ((VocableAdapter) adapter).remove(vocable);
            } else if (adapter instanceof UnitAdapter) {
                ((UnitAdapter) adapter).add(newUnit);
            }
        }
    }

    private void showVocableDialog(Integer unitId, Vocable vocable, Integer pos) {
        VocableDialog dialog = VocableDialog.newInstance(unitId, vocable, pos);

        dialog.setCallback(VocableListFragment.this);
        dialog.show(getFragmentManager(), VOCABLE_DIALOG);
    }

    @Override
    public void onUnitChanged(Unit unit, int pos) {
        vocableManager.updateUnit(unit);

        if (adapter instanceof UnitAdapter) {
            ((UnitAdapter) adapter).update(unit, pos);
        }
    }

    @Override
    public void onSort(SortMode mode) {
        this.mode = mode;

        /* adapter.setMode(mode);
        adapter.refresh(); */

        if (adapter instanceof UnitAdapter) {
            setUnitAdapter();
        } else if (adapter instanceof VocableAdapter) {
            setVocableAdapter(((VocableAdapter) adapter).getUnit());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == TransferActivity.REQUEST_EXPORT) {
                String path = data.getStringExtra("path");

                ExportDialog.newInstance(path).show(getFragmentManager(), EXPORT_DIALOG);
            } else if (requestCode == TransferActivity.REQUEST_IMPORT) {
                String path = data.getStringExtra("path");
                ImportDialog dialog = ImportDialog.newInstance(path);

                dialog.setListener(this);
                dialog.show(getFragmentManager(), IMPORT_DIALOG);
            }
        }
    }

    @Override
    public void onDelete() {
        vocableManager.clear();
        adapter.clear();
    }

    @Override
    public void onImportFinished(String result) {
        if (adapter instanceof VocableAdapter) {
            setVocableAdapter(((VocableAdapter) adapter).getUnit());
        } else if (adapter instanceof UnitAdapter) {
            setUnitAdapter();
        }
    }

    private UndoManager getUndoManager() {
        return Core.getInstance(getActivity()).getUndoManager();
    }
}

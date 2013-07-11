package com.grazz.pebblereactor;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.getpebble.android.kit.util.PebbleDictionary;

public abstract class DataProvider {

	public enum DataType {
		TYPE_UINT8, TYPE_UINT32
	}

	private ReactorService _reactor;

	private List<Integer> _dataIds = new LinkedList<Integer>();
	private SparseArray<DataType> _dataTypes = new SparseArray<DataType>();
	private SparseArray<Object> _dataValues = new SparseArray<Object>();
	private SparseArray<Object> _previousDataValues = new SparseArray<Object>();

	public DataProvider(ReactorService reactor) {
		_reactor = reactor;
	}

	protected void registerCommand(int commandId) {
		_reactor.registerCommand(commandId, this);
	}

	protected Context getContext() {
		return _reactor.getApplicationContext();
	}

	protected void setValue(int slotId, DataType type, Object value, Boolean forcePush) {
		if (!_dataIds.contains(slotId))
			_dataIds.add(slotId);
		_dataTypes.put(slotId, type);
		_dataValues.put(slotId, value);
		_reactor.requestPush(slotId, forcePush);
	}

	public void addValueToDictionary(PebbleDictionary dictionary, int slotId) {
		switch (_dataTypes.get(slotId)) {
		case TYPE_UINT8:
			Byte byteValue = Byte.parseByte(_dataValues.get(slotId).toString());
			dictionary.addUint8(slotId, byteValue);
			break;

		case TYPE_UINT32:
			Integer integerValue = Integer.parseInt(_dataValues.get(slotId).toString());
			dictionary.addUint32(slotId, integerValue);
			break;
		}
	}

	public void addValuesToDictionary(PebbleDictionary dictionary, List<Integer> requestedSlots) {
		for (Integer slotId : _dataIds) {
			Boolean shouldAdd = false;

			if (requestedSlots == null)
				shouldAdd = true;
			else if (requestedSlots.contains(slotId) && !_dataValues.get(slotId).equals(_previousDataValues.get(slotId))) {
				shouldAdd = true;
				_previousDataValues.put(slotId, _dataValues.get(slotId));
			}

			if (shouldAdd)
				addValueToDictionary(dictionary, slotId);
		}
	}

	public void addValuesToDictionary(PebbleDictionary dictionary) {
		addValuesToDictionary(dictionary, null);
	}

	public void onCommandReceived(int commandId) {
		Log.d("onCommandReceived", getClass().getSimpleName());
	}

	public abstract void onStart();

	public abstract void onStop();

}

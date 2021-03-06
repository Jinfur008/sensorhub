/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence;

import static org.junit.Assert.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.opengis.DateTimeDouble;
import net.opengis.IDateTime;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Quantity;
import org.junit.Test;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.test.TestUtils;
import org.vast.data.BinaryEncodingImpl;
import org.vast.data.CountImpl;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TextImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.util.DateTimeFormat;


/**
 * <p>
 * Abstract base for testing implementations of IBasicStorage.
 * The storage needs to be correctly instianted by derived tests in a method
 * tagged with '@Before'.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @param <StorageType> type of storage under test
 * @since Nov 29, 2014
 */
public abstract class AbstractTestBasicStorage<StorageType extends IBasicStorage<?>>
{
    protected StorageType storage;
    
    
    protected abstract void forceReadBackFromStorage();
    
    
    protected DataComponent createDs1() throws Exception
    {
        DataComponent recordDesc = new QuantityImpl();
        storage.addNewDataStore("ds1", recordDesc, new TextEncodingImpl());
        return recordDesc;
    }
    
    
    protected DataComponent createDs2() throws Exception
    {
        DataComponent recordDesc = new DataRecordImpl();
        recordDesc.setDefinition("urn:auth:blabla:record-stuff");
        Quantity q = new QuantityImpl();
        q.setLabel("My Quantity");
        q.getUom().setCode("m.s-2.kg-1");
        recordDesc.addComponent("c1", q);
        recordDesc.addComponent("c2", new CountImpl());
        recordDesc.addComponent("c3", new TextImpl());
        storage.addNewDataStore("ds2", recordDesc, new TextEncodingImpl());
        return recordDesc;
    }
    
    
    protected DataComponent createDs3(DataComponent nestedRec) throws Exception
    {
        DataComponent recordDesc = new DataArrayImpl(10);
        recordDesc.setDefinition("urn:auth:blabla:array-stuff");
        ((DataArray)recordDesc).setElementType("elt", nestedRec);
        storage.addNewDataStore("ds3", recordDesc, new BinaryEncodingImpl());
        return recordDesc;
    }
    
    
    @Test
    public void testCreateDataStores() throws Exception
    {
        Map<String, ? extends ITimeSeriesDataStore<?>> dataStores;
        String dataStoreName;
        
        DataComponent recordDs1 = createDs1();
        dataStores = storage.getDataStores();
        assertEquals(1, dataStores.size());
        
        DataComponent recordDs2 = createDs2();
        dataStores = storage.getDataStores();        
        assertEquals(2, dataStores.size());  
        
        forceReadBackFromStorage();
        dataStoreName = "ds1";
        TestUtils.assertEquals(recordDs1, dataStores.get(dataStoreName).getRecordDescription());
        assertEquals(TextEncodingImpl.class, dataStores.get(dataStoreName).getRecommendedEncoding().getClass());
        
        dataStoreName = "ds2";
        TestUtils.assertEquals(recordDs2, dataStores.get(dataStoreName).getRecordDescription());
        assertEquals(TextEncodingImpl.class, dataStores.get(dataStoreName).getRecommendedEncoding().getClass());
        
        dataStoreName = "ds3";
        DataComponent recordDs3 = createDs3(recordDs2);
        forceReadBackFromStorage();
        dataStores = storage.getDataStores();
        assertEquals(3, dataStores.size());        
        TestUtils.assertEquals(recordDs3, dataStores.get(dataStoreName).getRecordDescription());
        assertEquals(BinaryEncodingImpl.class, dataStores.get(dataStoreName).getRecommendedEncoding().getClass());
    }
    
    
    @Test
    public void testStoreAndGetLatestSensorML() throws Exception
    {
        SMLUtils smlUtils = new SMLUtils();
        InputStream is = new BufferedInputStream(getClass().getResourceAsStream("/gamma2070_more.xml"));
        AbstractProcess smlIn = smlUtils.readProcess(is);
        storage.storeDataSourceDescription(smlIn);
        forceReadBackFromStorage();
        AbstractProcess smlOut = storage.getLatestDataSourceDescription();
        TestUtils.assertEquals(smlIn, smlOut);
    }
    
    
    @Test
    public void testStoreAndGetSensorMLByTime() throws Exception
    {
        SMLUtils smlUtils = new SMLUtils();
        InputStream is;
                
        // load SensorML doc and set first validity period
        is = new BufferedInputStream(getClass().getResourceAsStream("/gamma2070_more.xml"));
        AbstractProcess smlIn1 = smlUtils.readProcess(is);
        IDateTime begin1 = new DateTimeDouble(new DateTimeFormat().parseIso("2010-05-15Z"));
        ((TimePeriod)smlIn1.getValidTimeList().get(0)).getBeginPosition().setDateTimeValue(begin1);
        IDateTime end1 = new DateTimeDouble(new DateTimeFormat().parseIso("2010-09-23Z"));
        ((TimePeriod)smlIn1.getValidTimeList().get(0)).getEndPosition().setDateTimeValue(end1);
        storage.storeDataSourceDescription(smlIn1);
        forceReadBackFromStorage();
        
        AbstractProcess smlOut = storage.getLatestDataSourceDescription();
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin1.getAsDouble());
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(end1.getAsDouble());
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin1.getAsDouble() + 3600*24*10);
        TestUtils.assertEquals(smlIn1, smlOut);
        
        // load SensorML doc another time and set with a different validity period
        is = new BufferedInputStream(getClass().getResourceAsStream("/gamma2070_more.xml"));
        AbstractProcess smlIn2 = smlUtils.readProcess(is);
        IDateTime begin2 = new DateTimeDouble(new DateTimeFormat().parseIso("2010-09-24Z"));
        ((TimePeriod)smlIn2.getValidTimeList().get(0)).getBeginPosition().setDateTimeValue(begin2);
        IDateTime end2 = new DateTimeDouble(new DateTimeFormat().parseIso("2010-12-08Z"));
        ((TimePeriod)smlIn2.getValidTimeList().get(0)).getEndPosition().setDateTimeValue(end2);
        storage.storeDataSourceDescription(smlIn2);        
        forceReadBackFromStorage();
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin1.getAsDouble());
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(end1.getAsDouble());
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin1.getAsDouble() + 3600*24*10);
        TestUtils.assertEquals(smlIn1, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin2.getAsDouble());
        TestUtils.assertEquals(smlIn2, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(end2.getAsDouble());
        TestUtils.assertEquals(smlIn2, smlOut);
        
        smlOut = storage.getDataSourceDescriptionAtTime(begin2.getAsDouble() + 3600*24*10);
        TestUtils.assertEquals(smlIn2, smlOut);
    }
    
    
    @Test
    public void testStoreAndGetRecordsByKey() throws Exception
    {
        Map<String, ? extends ITimeSeriesDataStore<?>> dataStores;
        String dataStoreName;
        DataBlock data;
        DataKey key;
        IDataRecord<DataKey> dbRec;
        
        // test data store #1
        dataStoreName = "ds1";
        data = createDs1().createDataBlock();
        data.setDoubleValue(0.95);
        dataStores = storage.getDataStores();
        key = new DataKey("proc1", 12.0);
        dataStores.get(dataStoreName).store(key, data);
        forceReadBackFromStorage();
        dbRec = dataStores.get(dataStoreName).getRecord(key);
        TestUtils.assertEquals(data, dbRec.getData());
        
        // test data store #2
        dataStoreName = "ds2";
        DataComponent recordDs2 = createDs2();
        data = recordDs2.createDataBlock();
        data.setDoubleValue(0, 1.0);
        data.setIntValue(1, 2);
        data.setStringValue(2, "test");
        dataStores = storage.getDataStores();
        key = new DataKey("proc2", 123.0);
        dataStores.get(dataStoreName).store(key, data);
        forceReadBackFromStorage();
        dbRec = dataStores.get(dataStoreName).getRecord(key);
        TestUtils.assertEquals(data, dbRec.getData());
        
        // test data store #3
        dataStoreName = "ds3";
        DataArray recordDs3 = (DataArray)createDs3(recordDs2);
        data = recordDs3.createDataBlock();
        int arraySize = recordDs3.getElementCount().getValue();
        int offset = 0;
        for (int i=0; i<arraySize; i++)
        {
            data.setDoubleValue(offset++, i+0.5);
            data.setIntValue(offset++, 2*i);
            data.setStringValue(offset++, "test" + i);
        }
        dataStores = storage.getDataStores();
        key = new DataKey("proc2", 10.);
        dataStores.get(dataStoreName).store(key, data);
        forceReadBackFromStorage();
        dbRec = dataStores.get(dataStoreName).getRecord(key);
        TestUtils.assertEquals(data, dbRec.getData());
    }
    
    
    @Test
    public void testStoreAndGetMultipleRecordsByKey() throws Exception
    {
        Map<String, ? extends ITimeSeriesDataStore<?>> dataStores;
        String dataStoreName;
        DataBlock data;
        DataKey key;
        IDataRecord<DataKey> dbRec;
        
        dataStoreName = "ds2";
        DataComponent recordDef = createDs2();
        dataStores = storage.getDataStores();
        
        // write N records
        double timeStep = 0.1;
        int numRecords = 100;
        List<DataBlock> dataList = new ArrayList<DataBlock>(numRecords);
        storage.setAutoCommit(false);
        for (int i=0; i<numRecords; i++)
        {
            data = recordDef.createDataBlock();
            data.setDoubleValue(0, i + 0.3);
            data.setIntValue(1, 2*i);
            data.setStringValue(2, "test" + i);
            dataList.add(data);
            key = new DataKey("proc", i*timeStep);
            dataStores.get(dataStoreName).store(key, data);
        }
        storage.commit();
        forceReadBackFromStorage();
        
        // retrieve them and check their values
        for (int i=0; i<numRecords; i++)
        {
            key = new DataKey(null, i*timeStep);
            dbRec = dataStores.get(dataStoreName).getRecord(key);
            TestUtils.assertEquals(dataList.get(i), dbRec.getData());
        }
    }
    
    
    @Test
    public void testStoreAndGetTimeRange() throws Exception
    {
        Map<String, ? extends ITimeSeriesDataStore<?>> dataStores;
        String dataStoreName;
        DataBlock data;
        DataKey key;
        
        dataStoreName = "ds2";
        DataComponent recordDef = createDs2();
        dataStores = storage.getDataStores();
        
        // write N records
        double timeStamp = 0.0;
        double timeStep = 0.1;
        int numRecords = 100;
        List<DataBlock> dataList = new ArrayList<DataBlock>(numRecords);
        storage.setAutoCommit(false);
        for (int i=0; i<numRecords; i++)
        {
            data = recordDef.createDataBlock();
            data.setDoubleValue(0, i + 0.3);
            data.setIntValue(1, 2*i);
            data.setStringValue(2, "test" + i);
            dataList.add(data);
            timeStamp = i*timeStep;
            key = new DataKey("proc", timeStamp);
            dataStores.get(dataStoreName).store(key, data);
        }
        storage.commit();
        forceReadBackFromStorage();
        
        // retrieve them and check their values
        double[] timeRange = dataStores.get(dataStoreName).getDataTimeRange();
        assertEquals("Invalid begin time", 0., timeRange[0], 0.0);
        assertEquals("Invalid end time", timeStamp, timeRange[1], 0.0);
    }
    
    
    @Test
    public void testStoreIncompatibleRecord() throws Exception
    {
        // TODO check that a datablock that is incompatible with record definition is rejected
    }

}

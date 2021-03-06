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

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Configuration class for SOS data providers using the persistence API.
 * This class is for a storage only data source. For connecting a sensor
 * with its own storage to an SOS service, use the SensorDataProviderConfig
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 14, 2013
 */
public class StorageDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of storage to use as data source
     */
    public String storageID;  
    
    
    /**
     * Names of data stores whose data will be hidden from the SOS
     * If this is null, all streams offered by storage are exposed
     */
    public List<String> hiddenOutputs = new ArrayList<String>();
    

    /*
     * Copy constructor to configure storage from sensor provider info
     */
    protected StorageDataProviderConfig(SensorDataProviderConfig sensorConfig)
    {
        this.enabled = sensorConfig.enabled;
        this.uri = sensorConfig.uri;
        this.name = sensorConfig.name;
        this.description = sensorConfig.description;
        this.storageID = sensorConfig.storageID;
        this.hiddenOutputs = sensorConfig.hiddenOutputs;
    }
    
    
    @Override
    protected IDataProviderFactory getFactory() throws SensorHubException
    {
        return new StorageDataProviderFactory(this);
    }
}

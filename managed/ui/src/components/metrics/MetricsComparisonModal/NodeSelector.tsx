import React, { Fragment, FC } from 'react';
import { Dropdown, MenuItem, FormControl } from 'react-bootstrap';

import { MetricConsts } from '../../metrics/constants';
import { isNonEmptyObject, isNonEmptyString } from '../../../utils/ObjectUtils';

interface NodeSelectorData {
  selectedUniverse: any | null;
  nodeItemChanged: any;
  nodeItemChangedOld: any;
  selectedNode: string;
  otherSelectedNode?: string | null;
  selectedRegionClusterUUID: string | null;
  selectedZoneName: string | null;
  isTopKMetricsEnabled: boolean;
  selectedRegionCode: string;
}

export const NodeSelector: FC<NodeSelectorData> = ({
  selectedUniverse,
  nodeItemChanged,
  nodeItemChangedOld,
  selectedNode,
  otherSelectedNode,
  selectedRegionClusterUUID,
  selectedZoneName,
  isTopKMetricsEnabled,
  selectedRegionCode
}) => {
  let nodeItems: any[] = [];
  let nodeItemsElement: any = [];
  let zone = '';
  let renderItem = null;
  let nodeData = null;
  const isDisabled = (selectedUniverse === MetricConsts.ALL);

  if (
    isNonEmptyObject(selectedUniverse) &&
    selectedUniverse !== MetricConsts.ALL &&
    selectedUniverse.universeDetails.nodeDetailsSet
  ) {
    nodeItems = selectedUniverse.universeDetails.nodeDetailsSet.sort((a: any, b: any) => {
      if (a.cloudInfo.az === null) {
        return -1;
      } else if (b.cloudInfo.az === null) {
        return 1;
      } else {
        return a.cloudInfo.az.toLowerCase() < b.cloudInfo.az.toLowerCase() ? -1 : 1;
      }
    });
  }

  if (isTopKMetricsEnabled) {
    // Show nodes based on the region selected (we filter this by cluster id)
    if (selectedRegionClusterUUID) {
      nodeItems = nodeItems.filter((nodeItem: any) => selectedRegionCode ? selectedRegionClusterUUID === nodeItem.placementUuid
        && nodeItem.cloudInfo?.region === selectedRegionCode : selectedRegionClusterUUID === nodeItem.placementUuid);
    }

    nodeItemsElement = nodeItems?.map((nodeItem: any, nodeIdx: number) => {
      let zoneNameElement = null;
      let zoneDividerElement = null;
      const nodeKey = `${nodeItem.nodeName}-node-${nodeIdx}`;
      const zoneKey = `${nodeItem.cloudInfo.az}-zone-${nodeIdx}`;
      let isZoneDivider = false;
      // Logic to decide when AZ and divider needs to be shown
      if (zone !== nodeItem.cloudInfo.az) {
        zoneDividerElement = isNonEmptyString(zone) ? (<div id="zone-divider" className="divider" />) : null;
        zoneNameElement = (
          <MenuItem
            onSelect={() => nodeItemChanged(MetricConsts.ALL, nodeItem.cloudInfo.az)}
            key={zoneKey}
            // Added this line due to the issue that dropdown does not close
            // when a menu item is selected
            onClick={() => { document.body.click(); }}
            eventKey={nodeItem.cloudInfo.az}
            active={selectedZoneName === nodeItem.cloudInfo.az}
          >
            <span className="cluster-az-name">{nodeItem.cloudInfo.az}</span>
          </MenuItem>
        );
        isZoneDivider = true;
        zone = nodeItem.cloudInfo.az;
      }

      return (
        <Fragment>
          {zoneNameElement}
          {nodeItems.length > 1 && isZoneDivider ? zoneDividerElement : null}
          <MenuItem
            onSelect={() => nodeItemChanged(nodeItem.nodeName, null)}
            key={nodeKey}
            // Added this line due to the issue that dropdown does not close
            // when a menu item is selected
            onClick={() => { document.body.click(); }}
            eventKey={nodeIdx}
            active={selectedNode === nodeItem.nodeName}
          >
            <span className={"node-name"}>
              {nodeItem.nodeName}
            </span>
            &nbsp;&nbsp;
            <span className={"node-ip-address"}>
              {nodeItem.cloudInfo.private_ip}
            </span>
            {nodeItem.nodeName === otherSelectedNode ? 'Already selected' : ''}
          </MenuItem>
        </Fragment>
      );
    });

    // By default we need to have 'All nodes' populated
    const defaultMenuItem = (
      <Fragment>
        <MenuItem
          onSelect={() => nodeItemChanged(MetricConsts.ALL, null)}
          key={MetricConsts.ALL}
          // Added this line due to the issue that dropdown does not close
          // when a menu item is selected
          active={selectedNode === MetricConsts.ALL && !selectedZoneName}
          onClick={() => { document.body.click(); }}
          eventKey={MetricConsts.ALL}
        >
          {'All AZs & nodes'}
        </MenuItem>
      </Fragment>
    );
    nodeItemsElement.splice(0, 0, defaultMenuItem);

    if (selectedZoneName) {
      renderItem = selectedZoneName;
    } else if (selectedNode && selectedNode !== MetricConsts.ALL) {
      renderItem = selectedNode;
    } else {
      renderItem = nodeItemsElement[0];
    }

    nodeData = (
      <div className="node-picker">
        <Dropdown
          id="nodeFilterDropdown"
          className="node-filter-dropdown"
          disabled={isDisabled}
          title={isDisabled ? "Select a specific universe to view the zones and nodes" : ""}
        >
          <Dropdown.Toggle className="dropdown-toggle-button">
            <span className="default-node-value">
              {renderItem}
            </span>
          </Dropdown.Toggle>
          <Dropdown.Menu>
            {nodeItemsElement.length > 1 ? <div id="all-divider" className="divider" /> : null}
            {nodeItemsElement.length > 1 && nodeItemsElement}
          </Dropdown.Menu>
        </Dropdown>
      </div>
    );
  } else {
    // TODO: Needs to be removed once Top K metrics is tested and integrated fully
    nodeData = (
      <div className="node-picker-old">
        <FormControl componentClass="select" onChange={nodeItemChangedOld} value={selectedNode}>
          <option key={-2} value="all">
            All
          </option>
          {nodeItems.map((nodeItem, nodeIdx) => (
            <option
              key={nodeIdx}
              value={nodeItem.nodeName}
              disabled={nodeItem.nodeName === otherSelectedNode}
            >
              {nodeItem.nodeName}
              {nodeItem.nodeName === otherSelectedNode ? ' Already selected' : ''}
            </option>
          ))}
        </FormControl>
      </div>
    );
  }
  return nodeData;
};

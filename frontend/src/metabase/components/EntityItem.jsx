import React from "react";
import { t } from "c-3po";
import EntityMenu from "metabase/components/EntityMenu";
import Swapper from "metabase/components/Swapper";
import IconWrapper from "metabase/components/IconWrapper";

import { Flex, Box, Truncate } from "rebass";

import CheckBox from "metabase/components/CheckBox";
import Icon from "metabase/components/Icon";

import { normal } from "metabase/lib/colors";

const EntityItemWrapper = Flex.extend`
  border-bottom: 1px solid #f8f9fa;
  /* TODO - figure out how to use the prop instead of this? */
  align-items: center;
  &:hover {
    color: ${normal.blue};
  }
  &:last-child {
    border-bottom: none;
  }
`;

const EntityItem = ({
  name,
  iconName,
  iconColor,
  isFavorite,
  onPin,
  onFavorite,
  onMove,
  onArchive,
  selected,
  onToggleSelected,
  selectable,
  showSelect,
}) => {
  const actions = [
    onPin && {
      title: t`Pin this item`,
      icon: "pin",
      action: onPin,
    },
    onMove && {
      title: t`Move this item`,
      icon: "move",
      action: onMove,
    },
    onArchive && {
      title: t`Archive`,
      icon: "archive",
      action: onArchive,
    },
  ].filter(action => action);

  return (
    <EntityItemWrapper py={2} px={2} className="hover-parent hover--visibility">
      <IconWrapper
        p={1}
        mr={1}
        align="center"
        justify="center"
        onClick={
          selectable
            ? e => {
                e.preventDefault();
                onToggleSelected();
              }
            : null
        }
      >
        {selectable ? (
          <Swapper
            startSwapped={showSelect}
            defaultElement={<Icon name={iconName} color={iconColor} />}
            swappedElement={<CheckBox checked={selected} />}
          />
        ) : (
          <Icon name={iconName} color={iconColor} />
        )}
      </IconWrapper>
      <h3>
        <Truncate>{name}</Truncate>
      </h3>

      <Flex
        ml="auto"
        align="center"
        className="hover-child"
        onClick={e => e.preventDefault()}
      >
        {(onFavorite || isFavorite) && (
          <Icon
            name={isFavorite ? "star" : "staroutline"}
            mr={1}
            onClick={onFavorite}
          />
        )}
        {actions.length > 0 && (
          <EntityMenu triggerIcon="ellipsis" items={actions} />
        )}
      </Flex>
    </EntityItemWrapper>
  );
};

EntityItem.defaultProps = {
  selectable: false,
};

export default EntityItem;

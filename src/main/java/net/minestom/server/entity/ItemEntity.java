package net.minestom.server.entity;

import net.minestom.server.event.entity.EntityItemMergeEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.StackingRule;
import net.minestom.server.network.packet.PacketWriter;
import net.minestom.server.utils.Position;
import net.minestom.server.utils.time.CooldownUtils;
import net.minestom.server.utils.time.TimeUnit;
import net.minestom.server.utils.time.UpdateOption;

import java.util.Set;
import java.util.function.Consumer;

public class ItemEntity extends ObjectEntity {

    /**
     * Used to slow down the merge check delay
     */
    private static UpdateOption mergeUpdateOption = new UpdateOption(10, TimeUnit.TICK);

    /**
     * The last time that this item has checked his neighbors for merge
     */
    private long lastMergeCheck;

    private ItemStack itemStack;

    private boolean pickable = true;
    private boolean mergeable = true;
    private float mergeRange = 1;

    private long spawnTime;
    private long pickupDelay;

    public ItemEntity(ItemStack itemStack, Position spawnPosition) {
        super(EntityType.ITEM, spawnPosition);
        this.itemStack = itemStack;
        setBoundingBox(0.25f, 0.25f, 0.25f);
    }

    /**
     * Get the update option for the merging feature
     *
     * @return the merge update option
     */
    public static UpdateOption getMergeUpdateOption() {
        return mergeUpdateOption;
    }

    /**
     * Change the merge update option.
     * Can be set to null to entirely remove the delay
     *
     * @param mergeUpdateOption the new merge update option
     */
    public static void setMergeUpdateOption(UpdateOption mergeUpdateOption) {
        ItemEntity.mergeUpdateOption = mergeUpdateOption;
    }

    @Override
    public void update(long time) {
        if (isMergeable() && isPickable() &&
                (mergeUpdateOption == null || !CooldownUtils.hasCooldown(time, lastMergeCheck, mergeUpdateOption))) {
            this.lastMergeCheck = time;

            Chunk chunk = instance.getChunkAt(getPosition());
            Set<Entity> entities = instance.getChunkEntities(chunk);
            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {

                    // Do not merge with itself
                    if (entity == this)
                        continue;

                    ItemEntity itemEntity = (ItemEntity) entity;
                    if (!itemEntity.isPickable() || !itemEntity.isMergeable())
                        continue;

                    // Too far, do not merge
                    if (getDistance(itemEntity) > mergeRange)
                        continue;

                    // Use the class as a monitor to prevent deadlock
                    // Shouldn't happen too often to be an issue
                    synchronized (ItemEntity.class) {
                        final ItemStack itemStackEntity = itemEntity.getItemStack();

                        final StackingRule stackingRule = itemStack.getStackingRule();
                        final boolean canStack = stackingRule.canBeStacked(itemStack, itemStackEntity);

                        if (!canStack)
                            continue;

                        final int totalAmount = stackingRule.getAmount(itemStack) + stackingRule.getAmount(itemStackEntity);
                        final boolean canApply = stackingRule.canApply(itemStack, totalAmount);

                        if (!canApply)
                            continue;

                        EntityItemMergeEvent entityItemMergeEvent = new EntityItemMergeEvent(this, itemEntity);
                        callCancellableEvent(EntityItemMergeEvent.class, entityItemMergeEvent, () -> {
                            ItemStack result = stackingRule.apply(itemStack.clone(), totalAmount);
                            setItemStack(result);
                            itemEntity.remove();
                        });
                    }

                }
            }
        }
    }

    @Override
    public void spawn() {
        this.spawnTime = System.currentTimeMillis();
    }

    @Override
    public Consumer<PacketWriter> getMetadataConsumer() {
        return packet -> {
            super.getMetadataConsumer().accept(packet);
            fillMetadataIndex(packet, 7);
        };
    }

    @Override
    protected void fillMetadataIndex(PacketWriter packet, int index) {
        super.fillMetadataIndex(packet, index);
        if (index == 7) {
            packet.writeByte((byte) 7);
            packet.writeByte(METADATA_SLOT);
            packet.writeItemStack(itemStack);
        }

    }

    @Override
    public int getObjectData() {
        return 1;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        sendMetadataIndex(7); // Refresh the ItemStack for viewers
    }

    public boolean isPickable() {
        return pickable && (System.currentTimeMillis() - getSpawnTime() >= pickupDelay);
    }

    public void setPickable(boolean pickable) {
        this.pickable = pickable;
    }

    /**
     * @return true if the entity is mergeable, false otherwise
     */
    public boolean isMergeable() {
        return mergeable;
    }

    /**
     * When set to true, close {@link ItemEntity} will try to merge together as a single entity
     * when their {@link #getItemStack()} is similar and allowed to stack together
     *
     * @param mergeable should the entity merge with other {@link ItemEntity}
     */
    public void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }

    public float getMergeRange() {
        return mergeRange;
    }

    public void setMergeRange(float mergeRange) {
        this.mergeRange = mergeRange;
    }

    /**
     * @return the pickup delay in milliseconds, defined by {@link #setPickupDelay(long, TimeUnit)}
     */
    public long getPickupDelay() {
        return pickupDelay;
    }

    /**
     * Set the pickup delay of the ItemEntity
     *
     * @param delay
     * @param timeUnit
     */
    public void setPickupDelay(long delay, TimeUnit timeUnit) {
        this.pickupDelay = timeUnit.toMilliseconds(delay);
    }

    /**
     * Used to know if the ItemEntity can be pickup
     *
     * @return the time in milliseconds since this entity has spawn
     */
    public long getSpawnTime() {
        return spawnTime;
    }
}

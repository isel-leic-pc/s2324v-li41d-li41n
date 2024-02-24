#ifndef _LIST_H_
#define _LIST_H_

#include <stdint.h>
#include <stdbool.h>

typedef struct list_entry
{
  struct list_entry *next;
  struct list_entry *prev;
} list_entry_t;

#define node_of(entry_pointer, type, entry_field) \
  ((type *)((uint8_t *)(entry_pointer) - (size_t)(&((type *)0)->entry_field)))

void list_init(list_entry_t *list);
bool list_is_empty(list_entry_t *list);
void list_remove(list_entry_t *list);
list_entry_t *list_remove_head(list_entry_t *list);
list_entry_t *list_peek_head(list_entry_t *list);
void list_add_tail(list_entry_t *list, list_entry_t *entry);
void list_add_before(list_entry_t *node_after, list_entry_t *entry);

#endif // _LIST_H_

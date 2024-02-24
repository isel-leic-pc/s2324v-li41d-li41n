#include <stdlib.h>
#include "list.h"

void list_init(list_entry_t *list)
{
  list->next = list->prev = list;
}

bool list_is_empty(list_entry_t *list)
{
  return list->next == list;
}

void list_remove(list_entry_t *entry)
{
  entry->prev->next = entry->next;
  entry->next->prev = entry->prev;
}

list_entry_t *list_remove_head(list_entry_t *list)
{
  list_entry_t *head = list->next;
  list_remove(head);
  return head;
}

list_entry_t *list_peek_head(list_entry_t *list)
{
  if (list->next == list)
  {
    return NULL;
  }
  return list->next;
}

void list_add_tail(list_entry_t *list, list_entry_t *entry)
{
  list_entry_t *tail = list->prev;
  tail->next = entry;
  entry->prev = tail;
  entry->next = list;
  list->prev = entry;
}

void list_add_before(list_entry_t *node, list_entry_t *entry)
{
  entry->next = node;
  entry->prev = node->prev;
  entry->prev->next = entry;
  node->prev = entry;
}
